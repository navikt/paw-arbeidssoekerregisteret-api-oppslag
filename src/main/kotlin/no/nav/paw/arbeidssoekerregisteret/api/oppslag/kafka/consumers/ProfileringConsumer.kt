package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.getunleash.Unleash
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.pauseOrResumeConsumer
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class ProfileringConsumer(
    private val topic: String,
    private val consumer: KafkaConsumer<Long, Profilering>,
    private val profileringService: ProfileringService,
    private val unleashClient: Unleash
) {
    private var wasConsumerToggleActive: Boolean = false

    fun start() {
        logger.info("Lytter på topic $topic")
        consumer.subscribe(listOf(topic))

        val pollingInterval = Duration.ofMillis(1000)

        while (true) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, topic, isConsumerToggleActive, wasConsumerToggleActive)
            wasConsumerToggleActive = isConsumerToggleActive

            if (isConsumerToggleActive) {
                getAndProcessBatch(
                    source = consumer,
                    pollingInterval = pollingInterval,
                    receiver = ::processAndCommitBatch
                )
            } else {
                Thread.sleep(1000)
            }
        }
    }

    private fun processAndCommitBatch(batch: Iterable<Profilering>) =
        try {
            profileringService.lagreBatch(batch)
            consumer.commitSync()
        } catch (error: Exception) {
            throw Exception("Feil ved konsumering av melding fra $topic", error)
        }
}
