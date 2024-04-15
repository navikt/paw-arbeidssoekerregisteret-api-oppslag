package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.createKafkaConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
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
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.jetbrains.exposed.sql.Database
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

    // OBO vs StS token
    val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

    // Arbeidssøkerperiode avhengigheter
    val arbeidssoekerperiodeRepository = ArbeidssoekerperiodeRepository(database)
    val scheduleGetAktivePerioderGaugeService = ScheduleGetAktivePerioderGaugeService(registry, arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeConsumer =
        BatchConsumer(
            config.kafka.periodeTopic,
            config.kafka.createKafkaConsumer(),
            arbeidssoekerperiodeService::lagreBatch
        )

    // Situasjon avhengigheter
    val opplysningerOmArbeidssoekerRepository = OpplysningerOmArbeidssoekerRepository(database)
    val opplysningerOmArbeidssoekerService = OpplysningerOmArbeidssoekerService(opplysningerOmArbeidssoekerRepository)
    val opplysningerOmArbeidssoekerConsumer =
        BatchConsumer(
            config.kafka.opplysningerOmArbeidssoekerTopic,
            config.kafka.createKafkaConsumer(),
            opplysningerOmArbeidssoekerService::lagreBatch
        )

    // Profilering avhengigheter
    val profileringRepository = ProfileringRepository(database)
    val profileringService = ProfileringService(profileringRepository)
    val profileringConsumer =
        BatchConsumer(
            config.kafka.profileringTopic,
            config.kafka.createKafkaConsumer(),
            profileringService::lagreBatch
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
    val arbeidssoekerperiodeConsumer: BatchConsumer<Long, Periode>,
    val opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    val opplysningerOmArbeidssoekerConsumer: BatchConsumer<Long, OpplysningerOmArbeidssoeker>,
    val profileringService: ProfileringService,
    val profileringConsumer: BatchConsumer<Long, Profilering>,
    val autorisasjonService: AutorisasjonService,
    val scheduleGetAktivePerioderGaugeService: ScheduleGetAktivePerioderGaugeService
)
