package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.createKafkaConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.ArbeidssoekerperiodeConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.OpplysningerOmArbeidssoekerConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.ProfileringConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.metrics.ScheduleGetAktivePerioderGaugeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.TokenService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.generateDatasource
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getUnleashMock
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.isLocalEnvironment
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.jetbrains.exposed.sql.Database
import java.lang.System.getenv
import javax.sql.DataSource

fun createDependencies(config: Config): Dependencies {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dataSource = generateDatasource(config.database.url)

    val database = Database.connect(dataSource)

    val tokenService =
        config.authProviders.find {
            it.name == "azure"
        }?.run(::TokenService) ?: throw RuntimeException("Azure provider ikke funnet")

    val poaoTilgangHttpClient =
        PoaoTilgangCachedClient(
            PoaoTilgangHttpClient(
                config.poaoClientConfig.url,
                { tokenService.createMachineToMachineToken(config.poaoClientConfig.scope) }
            )
        )

    val unleashClient =
        if (isLocalEnvironment()) {
            getUnleashMock()
        } else {
            DefaultUnleash(
                UnleashConfig.builder()
                    .appName(getenv("NAIS_APP_NAME"))
                    .instanceId(getenv("NAIS_APP_NAME"))
                    .unleashAPI(getenv("UNLEASH_SERVER_API_URL"))
                    .apiKey(getenv("UNLEASH_SERVER_API_TOKEN"))
                    .build()
            )
        }

    // OBO vs StS token
    val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

    // Arbeidssøkerperiode avhengigheter
    val arbeidssoekerperiodeRepository = ArbeidssoekerperiodeRepository(database)
    val scheduleGetAktivePerioderGaugeService = ScheduleGetAktivePerioderGaugeService(registry, arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeConsumer =
        ArbeidssoekerperiodeConsumer(
            config.kafka.periodeTopic,
            config.kafka.createKafkaConsumer(),
            arbeidssoekerperiodeService,
            unleashClient
        )

    // Situasjon avhengigheter
    val opplysningerOmArbeidssoekerRepository = OpplysningerOmArbeidssoekerRepository(database)
    val opplysningerOmArbeidssoekerService = OpplysningerOmArbeidssoekerService(opplysningerOmArbeidssoekerRepository)
    val opplysningerOmArbeidssoekerConsumer =
        OpplysningerOmArbeidssoekerConsumer(
            config.kafka.opplysningerOmArbeidssoekerTopic,
            config.kafka.createKafkaConsumer(),
            opplysningerOmArbeidssoekerService,
            unleashClient
        )

    // Profilering avhengigheter
    val profileringRepository = ProfileringRepository(database)
    val profileringService = ProfileringService(profileringRepository)
    val profileringConsumer =
        ProfileringConsumer(
            config.kafka.profileringTopic,
            config.kafka.createKafkaConsumer(),
            profileringService,
            unleashClient
        )

    return Dependencies(
        registry,
        dataSource,
        arbeidssoekerperiodeService,
        arbeidssoekerperiodeConsumer,
        opplysningerOmArbeidssoekerService,
        opplysningerOmArbeidssoekerConsumer,
        profileringService,
        profileringConsumer,
        autorisasjonService,
        scheduleGetAktivePerioderGaugeService
    )
}

data class Dependencies(
    val registry: PrometheusMeterRegistry,
    val dataSource: DataSource,
    val arbeidssoekerperiodeService: ArbeidssoekerperiodeService,
    val arbeidssoekerperiodeConsumer: ArbeidssoekerperiodeConsumer,
    val opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    val opplysningerOmArbeidssoekerConsumer: OpplysningerOmArbeidssoekerConsumer,
    val profileringService: ProfileringService,
    val profileringConsumer: ProfileringConsumer,
    val autorisasjonService: AutorisasjonService,
    val scheduleGetAktivePerioderGaugeService: ScheduleGetAktivePerioderGaugeService
)
