package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.getunleash.Unleash
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.pauseOrResumeConsumer
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class OpplysningerOmArbeidssoekerConsumer(
    private val topic: String,
    private val consumer: KafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    private val opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    private val unleashClient: Unleash
) {
    private var wasConsumerToggleActive: Boolean = false

    fun start() {
        logger.info("Lytter på topic $topic")
        consumer.subscribe(listOf(topic))

        val pollingInterval = Duration.ofMillis(100)

        while (true) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, topic, isConsumerToggleActive, wasConsumerToggleActive)
            wasConsumerToggleActive = isConsumerToggleActive

            if (isConsumerToggleActive) {
                val records: ConsumerRecords<Long, OpplysningerOmArbeidssoeker> =
                    consumer.poll(pollingInterval)
                        .onEach {
                            logger.info("Mottok melding fra $topic med offset ${it.offset()} partition ${it.partition()}")
                        }
                val opplysninger =
                    records.map { record: ConsumerRecord<Long, OpplysningerOmArbeidssoeker> ->
                        record.value()
                    }
                processAndCommitBatch(opplysninger)
            }

            Thread.sleep(1000)
        }
    }

    private fun processAndCommitBatch(batch: Iterable<OpplysningerOmArbeidssoeker>) =
        try {
            opplysningerOmArbeidssoekerService.lagreBatch(batch)
            consumer.commitSync()
        } catch (error: Exception) {
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
}
