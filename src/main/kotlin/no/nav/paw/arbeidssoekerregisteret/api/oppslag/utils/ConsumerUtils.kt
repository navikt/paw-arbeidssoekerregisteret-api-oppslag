package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger

fun pauseOrResumeConsumer(
    consumer: KafkaConsumer<String, *>,
    isConsumerToggleActive: Boolean,
    wasConsumerToggleActive: Boolean,
    logger: Logger,
    topic: String
) {
    if (wasConsumerToggleActive != isConsumerToggleActive) {
        if (isConsumerToggleActive) {
            logger.info("Konsumering av $topic er aktivert")
            consumer.resume(consumer.assignment())
        } else {
            logger.info("Konsumering av $topic er deaktivert")
            consumer.pause(consumer.assignment())
        }
    }
}
