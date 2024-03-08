package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration

@WithSpan(
    value = "get_and_process_batch",
    kind = SpanKind.CONSUMER
)
fun <K, V> getAndProcessBatch(
    source: Consumer<K, V>,
    pollingInterval: Duration,
    receiver: (Iterable<V>) -> Unit,
    @Suppress("UNUSED_PARAMETER")
    @SpanAttribute("topics") topics: String = source.listTopics().keys.joinToString(",")
) {
    val records: ConsumerRecords<K, V> =
        source.poll(pollingInterval)
            .onEach {
                logger.info("Mottok melding fra ${it.topic()} med offset ${it.offset()} partition ${it.partition()}")
            }
    val values = records.map { it.value() }
    receiver(values)
}
