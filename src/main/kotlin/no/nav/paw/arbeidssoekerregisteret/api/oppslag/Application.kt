package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.loadConfiguration
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.migrateDatabase
import java.util.concurrent.CompletableFuture.runAsync
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    // Konfigurasjon
    val config = loadConfiguration<Config>()
    // Avhengigheter
    val dependencies = createDependencies(config)
    // Konsumer periode meldinger fra Kafka
    // Clean database pga versjon oppdatering
    // cleanDatabase(dependencies.dataSource) // TODO: Fjern denne ved neste commit
    // Kjør migration på database
    migrateDatabase(dependencies.dataSource)
    thread {
        try {
            dependencies.arbeidssoekerperiodeConsumer.subscribe()
            dependencies.opplysningerOmArbeidssoekerConsumer.subscribe()
            dependencies.profileringConsumer.subscribe()
            while (true) {
                consume(dependencies, config)
            }
        } catch (e: Exception) {
            logger.error("Arbeidssøkerperiode consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }
    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        dependencies.scheduleGetAktivePerioderGaugeService.scheduleGetAktivePerioderTask()
    }
    val server =
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = { module(dependencies, config) })
            .start(wait = true)

    server.addShutdownHook {
        server.stop(300, 300)
    }
}

fun Application.module(
    dependencies: Dependencies,
    config: Config
) {
    // Konfigurerer plugins
    configureMetrics(dependencies.registry)
    configureHTTP()
    configureAuthentication(config.authProviders)
    configureLogging()
    configureSerialization()

    // Ruter
    routing {
        healthRoutes(dependencies.registry)
        swaggerRoutes()
        arbeidssokerRoutes(
            dependencies.autorisasjonService,
            dependencies.arbeidssoekerperiodeService,
            dependencies.opplysningerOmArbeidssoekerService,
            dependencies.profileringService
        )
    }
}

@WithSpan(
    value = "consume",
    kind = SpanKind.INTERNAL
)
fun consume(
    dependencies: Dependencies,
    config: Config
) {
    listOf(
        runAsync {
            dependencies.arbeidssoekerperiodeConsumer.getAndProcessBatch(config.kafka.periodeTopic)
        },
        runAsync {
            dependencies.opplysningerOmArbeidssoekerConsumer.getAndProcessBatch(config.kafka.opplysningerOmArbeidssoekerTopic)
        },
        runAsync {
            dependencies.profileringConsumer.getAndProcessBatch(config.kafka.profileringTopic)
        }
    ).forEach { it.join() }
}
