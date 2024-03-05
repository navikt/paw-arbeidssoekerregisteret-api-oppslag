package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.getunleash.Unleash
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.pauseOrResumeConsumer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class ArbeidssoekerperiodeConsumer(
    private val topic: String,
    private val consumer: KafkaConsumer<Long, Periode>,
    private val arbeidssoekerperiodeService: ArbeidssoekerperiodeService,
    private val unleashClient: Unleash
) {
    private var wasConsumerToggleActive: Boolean = false

    fun start() {
        logger.info("Lytter på topic $topic")
        consumer.subscribe(listOf(topic))

        val pollingInterval = Duration.ofMillis(100)
        val maxBatchSize = 100
        val batch = mutableListOf<Periode>()

        while (true) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, isConsumerToggleActive, wasConsumerToggleActive, logger, topic)
            wasConsumerToggleActive = isConsumerToggleActive

            if (isConsumerToggleActive) {
                val records: ConsumerRecords<Long, Periode> = consumer.poll(pollingInterval)
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
    private fun processAndCommitBatch(batch: List<Periode>) {
        try {
            logger.info("Lagrer batch med ${batch.size} meldinger fra $topic")

            arbeidssoekerperiodeService.beginTransaction()
            arbeidssoekerperiodeService.storeBatch(batch)
            arbeidssoekerperiodeService.commitTransaction()

            logger.info("Batch med ${batch.size} meldinger fra $topic lagret")

            consumer.commitSync()
        } catch (error: Exception) {
            arbeidssoekerperiodeService.rollbackTransaction()
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
    }
}
