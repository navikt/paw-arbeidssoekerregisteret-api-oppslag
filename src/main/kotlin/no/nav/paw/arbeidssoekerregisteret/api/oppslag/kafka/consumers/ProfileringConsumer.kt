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
    private val consumer: KafkaConsumer<String, Profilering>,
    private val profileringService: ProfileringService,
    private val unleashClient: Unleash
) {
    private var wasConsumerToggleActive: Boolean = false

    fun start() {
        logger.info("Lytter på topic $topic")
        consumer.subscribe(listOf(topic))

        while (true) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, isConsumerToggleActive, wasConsumerToggleActive, logger, topic)
            wasConsumerToggleActive = isConsumerToggleActive

            if (isConsumerToggleActive) {
                consumer.poll(Duration.ofMillis(500)).forEach { post ->
                    try {
                        logger.info("Mottok melding fra $topic med offset ${post.offset()} partition ${post.partition()}")
                        val profileringAvArbeidssoeker = post.value()
                        profileringService.opprettProfileringForArbeidssoeker(profileringAvArbeidssoeker)

                        consumer.commitSync()
                    } catch (error: Exception) {
                        throw Exception("Feil ved konsumering av melding fra $topic", error)
                    }
                }
            }

            Thread.sleep(1000)
        }
    }
}