package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val logger: Logger get() = LoggerFactory.getLogger("ConsumerLogger")

fun pauseOrResumeConsumer(
    consumer: KafkaConsumer<*, *>,
    topic: String,
    isConsumerToggleActive: Boolean,
    wasConsumerToggleActive: Boolean
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
