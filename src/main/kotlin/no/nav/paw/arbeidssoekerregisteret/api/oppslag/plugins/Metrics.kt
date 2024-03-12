package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.configureMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    vararg consumer: KafkaConsumer<*, *>
) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders =
            listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics()
            ) + consumer.map { KafkaClientMetrics(it) }
    }
}
