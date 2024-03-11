package no.nav.paw.arbeidssokerregisteret.api.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.hentTestPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.initTestDatabase
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class OpplysningerOmArbeidssoekerRepositoryTest : StringSpec({

    lateinit var dataSource: DataSource
    lateinit var database: Database
    val periodeId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb")
    val periodeId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fec")
    val opplysningerOmArbeidssoekerId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fed")
    val opplysningerOmArbeidssoekerId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fee")

    beforeEach {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        settInnTestPeriode(database, periodeId1)
        settInnTestPeriode(database, periodeId2)
    }

    afterEach {
        dataSource.connection.close()
    }

    "Opprett og hent ut opplysninger om arbeidssøker" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger = hentTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId1)

        retrievedOpplysninger.size shouldBe 1
        retrievedPeriodeOpplysninger.size shouldBe 1
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning, helse og annet lik null" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger = hentTestOpplysningerOmArbeidssoekerMedUtdanningHelseOgAnnetLikNull(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId1)

        retrievedOpplysninger.size shouldBe 1
        retrievedPeriodeOpplysninger.size shouldBe 1
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning og annet felter lik null" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger = hentTestOpplysningerOmArbeidssoekerMedUtdanningOgAnnetFelterLikNull(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId1)

        retrievedOpplysninger.size shouldBe 1
        retrievedPeriodeOpplysninger.size shouldBe 1
    }

    "Opprett og hent ut flere opplysninger om arbeidssøker med samme periodeId" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger1 = hentTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = hentTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId2)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(periodeId2)
        val retrievedPeriodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId2)

        retrievedOpplysninger.size shouldBe 2
        retrievedPeriodeOpplysninger.size shouldBe 2
    }

    "Opprett og hent ut opplysninger om arbeidssøker med forskjellig periodeId" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger1 = hentTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = hentTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId1)

        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val allePeriodeOpplysnigner = repository.hentAllePeriodeOpplysninger()
        val alleOpplysningerOm = repository.hentAlleOpplysningerOmArbeidssoeker()

        alleOpplysningerOm.size shouldBe 1
        allePeriodeOpplysnigner.size shouldBe 2
    }

    "Like opplysninger med samme periodeId skal ikke lagres på nytt" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)
        val opplysninger1 = hentTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = hentTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)

        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger1.periodeId)
        val retrievedPeriodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId1)

        retrievedOpplysninger.size shouldBe 1
        retrievedPeriodeOpplysninger.size shouldBe 1
    }

    "Hent ut ikke-eksisterende opplysninger om arbeidssøker" {
        val repository = OpplysningerOmArbeidssoekerRepository(database)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(UUID.randomUUID())

        retrievedOpplysninger.size shouldBe 0
    }
})

fun settInnTestPeriode(
    database: Database,
    periodeId: UUID
) {
    val arbeidssoekerperiodeRepository = ArbeidssoekerperiodeRepository(database)
    val periode = hentTestPeriode(periodeId)
    arbeidssoekerperiodeRepository.opprettArbeidssoekerperiode(periode)
}

fun hentTestOpplysningerOmArbeidssoeker(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
) = OpplysningerOmArbeidssoeker(
    opplysningerOmArbeidssoekerId,
    periodeId,
    Metadata(
        Instant.now(),
        Bruker(
            BrukerType.SYSTEM,
            "12345678911"
        ),
        "test",
        "test"
    ),
    Utdanning(
        "NUS_KODE",
        JaNeiVetIkke.VET_IKKE,
        JaNeiVetIkke.VET_IKKE
    ),
    Helse(
        JaNeiVetIkke.VET_IKKE
    ),
    Jobbsituasjon(
        listOf(
            BeskrivelseMedDetaljer(
                Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                hentMapAvDetaljer()
            ),
            BeskrivelseMedDetaljer(
                Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR,
                hentMapAvDetaljer()
            )
        )
    ),
    Annet(
        JaNeiVetIkke.VET_IKKE
    )
)

fun hentTestOpplysningerOmArbeidssoekerMedUtdanningHelseOgAnnetLikNull(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
): OpplysningerOmArbeidssoeker {
    return OpplysningerOmArbeidssoeker(
        opplysningerOmArbeidssoekerId,
        periodeId,
        Metadata(
            Instant.now(),
            Bruker(
                BrukerType.UKJENT_VERDI,
                "12345678911"
            ),
            "test",
            "test"
        ),
        null,
        null,
        Jobbsituasjon(
            listOf(
                BeskrivelseMedDetaljer(
                    Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                    mapOf(
                        Pair("test", "test"),
                        Pair("test2", "test2")
                    )
                ),
                BeskrivelseMedDetaljer(
                    Beskrivelse.DELTIDSJOBB_VIL_MER,
                    mapOf(
                        Pair("test3", "test3"),
                        Pair("test4", "test4")
                    )
                )
            )
        ),
        null
    )
}

fun hentTestOpplysningerOmArbeidssoekerMedUtdanningOgAnnetFelterLikNull(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
): OpplysningerOmArbeidssoeker {
    return OpplysningerOmArbeidssoeker(
        opplysningerOmArbeidssoekerId,
        periodeId,
        Metadata(
            Instant.now(),
            Bruker(
                BrukerType.UKJENT_VERDI,
                "12345678911"
            ),
            "test",
            "test"
        ),
        Utdanning(
            "NUS_KODE",
            null,
            null
        ),
        Helse(
            JaNeiVetIkke.JA
        ),
        Jobbsituasjon(
            listOf(
                BeskrivelseMedDetaljer(
                    Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                    mapOf(
                        Pair("test", "test"),
                        Pair("test2", "test2")
                    )
                ),
                BeskrivelseMedDetaljer(
                    Beskrivelse.DELTIDSJOBB_VIL_MER,
                    mapOf(
                        Pair("test3", "test3"),
                        Pair("test4", "test4")
                    )
                )
            )
        ),
        Annet(
            null
        )
    )
}

fun hentMapAvDetaljer(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    map["noekkel1"] = "verdi1"
    map["noekkel2"] = "verdi2"
    return map
}
