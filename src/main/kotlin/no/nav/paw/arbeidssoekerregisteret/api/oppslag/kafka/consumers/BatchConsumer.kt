package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.getunleash.Unleash
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.pauseOrResumeConsumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class BatchConsumer<K, V>(
    private val topic: String,
    val consumer: KafkaConsumer<K, V>,
    private val receiver: (Iterable<V>) -> Unit,
    private val unleashClient: Unleash
) {
    private val pollingInterval = Duration.ofMillis(100)
    private var wasConsumerToggleActive: Boolean = false
    private var isConsumerActive: Boolean = false

    fun start() {
        isConsumerActive = true
        logger.info("Lytter på topic $topic")
        subscribe()
        while (isConsumerActive) {
            val isConsumerToggleActive = unleashClient.isEnabled("aktiver-kafka-konsumere")
            pauseOrResumeConsumer(consumer, topic, isConsumerToggleActive, wasConsumerToggleActive)
            wasConsumerToggleActive = isConsumerToggleActive
            if (isConsumerToggleActive) {
                getAndProcessBatch()
            } else {
                Thread.sleep(1000)
            }
        }
    }

    fun stop() {
        isConsumerActive = false
    }

    fun subscribe() {
        consumer.subscribe(listOf(topic))
    }

    @WithSpan(
        value = "get_and_process_batch",
        kind = SpanKind.CONSUMER
    )
    fun getAndProcessBatch(
        @SpanAttribute("topics") topic: String = this.topic
    ) {
        consumer
            .poll(pollingInterval)
            .onEach {
                logger.trace(
                    "Mottok melding fra {} med offset {} partition {}",
                    it.topic(),
                    it.offset(),
                    it.partition()
                )
            }.map { it.value() }
            .runCatching(receiver)
            .mapCatching { commitSync() }
            .fold(
                onSuccess = { logger.debug("Batch behandlet og commitet") },
                onFailure = { error -> throw Exception("Feil ved konsumering av melding fra $topic", error) }
            )
    }

    @WithSpan(
        value = "commit_sync",
        kind = SpanKind.CLIENT
    )
    private fun commitSync() {
        consumer.commitSync()
    }
}
