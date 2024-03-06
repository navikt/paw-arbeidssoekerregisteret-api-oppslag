package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.getunleash.Unleash
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.pauseOrResumeConsumer
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
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
        val maxBatchSize = 100
        val batch = mutableListOf<OpplysningerOmArbeidssoeker>()

        while (true) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, topic, isConsumerToggleActive, wasConsumerToggleActive)
            wasConsumerToggleActive = isConsumerToggleActive

            if (isConsumerToggleActive) {
                val records: ConsumerRecords<Long, OpplysningerOmArbeidssoeker> = consumer.poll(pollingInterval)
                val recordIterator = records.iterator()

                while (recordIterator.hasNext()) {
                    val record = recordIterator.next()
                    logger.info("Mottok melding fra $topic med offset ${record.offset()} partition ${record.partition()}")
                    batch.add(record.value())

                    if (batch.size >= maxBatchSize || !recordIterator.hasNext()) {
                        processAndCommitBatch(batch)
                        batch.clear()
                    }
                }
            }

            Thread.sleep(1000)
        }
    }

    private fun processAndCommitBatch(batch: List<OpplysningerOmArbeidssoeker>) =
        try {
            logger.info("Lagrer batch med ${batch.size} meldinger fra $topic")

            opplysningerOmArbeidssoekerService.lagreBatch(batch)

            logger.info("Batch med ${batch.size} meldinger fra $topic lagret")

            consumer.commitSync()
        } catch (error: Exception) {
            opplysningerOmArbeidssoekerService.rollbackTransaction()
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
}
