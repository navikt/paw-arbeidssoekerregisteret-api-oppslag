package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.cleanDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.loadConfiguration
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.migrateDatabase
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    // Konfigurasjon
    val config = loadConfiguration<Config>()
    // Avhengigheter
    val dependencies = createDependencies(config)
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
    // Clean database pga versjon oppdatering
    cleanDatabase(dependencies.dataSource) // TODO: Fjern denne ved neste commit
    // Kjør migration på database
    migrateDatabase(dependencies.dataSource)

    // Konfigurerer plugins
    configureMetrics(dependencies.registry)
    configureHTTP()
    configureAuthentication(config.authProviders)
    configureLogging()
    configureSerialization()

    // Konsumer periode meldinger fra Kafka
    thread {
        try {
            dependencies.arbeidssoekerperiodeConsumer.start()
        } catch (e: Exception) {
            logger.error("Arbeidssøkerperiode consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }

    // Konsumer opplysninger-om-arbeidssøker meldinger fra Kafka
    thread {
        try {
            dependencies.opplysningerOmArbeidssoekerConsumer.start()
        } catch (e: Exception) {
            logger.error("Opplysninger-om-arbeidssøker consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }

    // Konsumer profilering meldinger fra Kafka
    thread {
        try {
            dependencies.profileringConsumer.start()
        } catch (e: Exception) {
            logger.error("Profilering consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }

    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        dependencies.scheduleGetAktivePerioderGaugeService.scheduleGetAktivePerioderTask()
    }

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
