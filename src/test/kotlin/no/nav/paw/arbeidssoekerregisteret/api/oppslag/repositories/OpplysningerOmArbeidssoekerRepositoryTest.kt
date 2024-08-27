package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class OpplysningerOmArbeidssoekerRepositoryTest : StringSpec({

    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var repository: OpplysningerOmArbeidssoekerRepository
    val periodeId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb")
    val periodeId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fec")
    val opplysningerOmArbeidssoekerId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fed")
    val opplysningerOmArbeidssoekerId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fee")

    beforeEach {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        repository = OpplysningerOmArbeidssoekerRepository(database)
        settInnTestPeriode(database, periodeId1)
        settInnTestPeriode(database, periodeId2)
    }

    afterEach {
        dataSource.connection.close()
    }

    "Opprett og hent ut opplysninger om arbeidssøker" {
        val opplysninger = lagTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = hentPeriodeOpplysninger(database, periodeId1)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning, helse og annet lik null" {
        val opplysninger = lagTestOpplysningerOmArbeidssoekerMedUtdanningHelseOgAnnetLikNull(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = hentPeriodeOpplysninger(database, periodeId1)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning og annet felter lik null" {
        val opplysninger = lagTestOpplysningerOmArbeidssoekerMedUtdanningOgAnnetFelterLikNull(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger.periodeId)
        val retrievedPeriodeOpplysninger = hentPeriodeOpplysninger(database, periodeId1)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut flere opplysninger om arbeidssøker med samme periodeId" {
        val opplysninger1 = lagTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = lagTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId2)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(periodeId2)
        val retrievedPeriodeOpplysninger = hentPeriodeOpplysninger(database, periodeId2)

        retrievedOpplysninger.size shouldBe 2
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        val retrievedOpplysninger2 = retrievedOpplysninger[1]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedOpplysninger2 shouldBeEqualTo opplysninger2
        retrievedPeriodeOpplysninger.size shouldBe 2
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        val retrievedPeriodeOpplysninger2 = retrievedPeriodeOpplysninger[1]
        retrievedPeriodeOpplysninger1[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger1.periodeId
        retrievedPeriodeOpplysninger2[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger2.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med forskjellig periodeId" {
        val opplysninger1 = lagTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = lagTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId1)

        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val allePeriodeOpplysnigner = hentAllePeriodeOpplysninger(database)
        val alleOpplysningerOm = hentAlleOpplysningerOmArbeidssoeker(database)

        alleOpplysningerOm.size shouldBe 1
        allePeriodeOpplysnigner.size shouldBe 2
    }

    "Like opplysninger med samme periodeId skal ikke lagres på nytt" {
        val opplysninger1 = lagTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = lagTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)

        repository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        repository.lagreOpplysningerOmArbeidssoeker(opplysninger2)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(opplysninger1.periodeId)
        val retrievedPeriodeOpplysninger = hentPeriodeOpplysninger(database, periodeId1)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1[PeriodeOpplysningerTable.periodeId] shouldBe retrievedOpplysninger1.periodeId
    }

    "Hent ut ikke-eksisterende opplysninger om arbeidssøker" {
        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(UUID.randomUUID())

        retrievedOpplysninger.size shouldBe 0
    }

    "Lagre opplysninger med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val opplysninger1 = lagTestOpplysningerOmArbeidssoeker(periodeId, UUID.randomUUID())
        val opplysninger2 = lagTestOpplysningerOmArbeidssoeker(periodeId, UUID.randomUUID())
        val opplysninger3 = lagTestOpplysningerOmArbeidssoeker(periodeId, UUID.randomUUID())
        val opplysninger = sequenceOf(opplysninger1, opplysninger2, opplysninger3)
        repository.storeBatch(opplysninger)

        val retrievedOpplysninger = repository.hentOpplysningerOmArbeidssoeker(periodeId)

        retrievedOpplysninger.size shouldBe 3
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        val retrievedOpplysninger2 = retrievedOpplysninger[1]
        val retrievedOpplysninger3 = retrievedOpplysninger[2]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedOpplysninger2 shouldBeEqualTo opplysninger2
        retrievedOpplysninger3 shouldBeEqualTo opplysninger3
    }
})

fun hentPeriodeOpplysninger(
    database: Database,
    periodeId: UUID
) = transaction(database) {
    PeriodeOpplysningerTable.selectAll()
        .where { PeriodeOpplysningerTable.periodeId eq periodeId }
        .toList()
}

fun hentAllePeriodeOpplysninger(database: Database) =
    transaction(database) {
        PeriodeOpplysningerTable.selectAll()
            .toList()
    }

fun hentAlleOpplysningerOmArbeidssoeker(database: Database) =
    transaction(database) {
        OpplysningerOmArbeidssoekerTable.selectAll()
            .toList()
    }

fun settInnTestPeriode(
    database: Database,
    periodeId: UUID
) {
    val arbeidssoekerperiodeRepository = ArbeidssoekerperiodeRepository(database)
    val periode = nyAvsluttetPeriode(periodeId = periodeId)
    arbeidssoekerperiodeRepository.opprettArbeidssoekerperiode(periode)
}

fun lagTestOpplysningerOmArbeidssoeker(
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
        "kilde",
        "aarsak",
        TidspunktFraKilde(
            Instant.now(),
            AvviksType.UKJENT_VERDI
        )
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
                mapOf(
                    Pair("noekkel1", "verdi1"),
                    Pair("noekkel2", "verdi2")
                )
            ),
            BeskrivelseMedDetaljer(
                Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR,
                mapOf(
                    Pair("noekkel3", "verdi3"),
                    Pair("noekkel4", "verdi4")
                )
            )
        )
    ),
    Annet(
        JaNeiVetIkke.VET_IKKE
    )
)

fun lagTestOpplysningerOmArbeidssoekerMedUtdanningHelseOgAnnetLikNull(
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
            "kilde",
            "aarsak",
            TidspunktFraKilde(
                Instant.now(),
                AvviksType.UKJENT_VERDI
            )
        ),
        null,
        null,
        Jobbsituasjon(
            listOf(
                BeskrivelseMedDetaljer(
                    Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                    mapOf(
                        Pair("noekkel1", "verdi1"),
                        Pair("noekkel2", "verdi2")
                    )
                ),
                BeskrivelseMedDetaljer(
                    Beskrivelse.DELTIDSJOBB_VIL_MER,
                    mapOf(
                        Pair("noekkel3", "verdi3"),
                        Pair("noekkel4", "verdi4")
                    )
                )
            )
        ),
        null
    )
}

fun lagTestOpplysningerOmArbeidssoekerMedUtdanningOgAnnetFelterLikNull(
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
            "kilde",
            "aarsak",
            TidspunktFraKilde(
                Instant.now(),
                AvviksType.UKJENT_VERDI
            )
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
                        Pair("noekkel1", "verdi1"),
                        Pair("noekkel2", "verdi2")
                    )
                ),
                BeskrivelseMedDetaljer(
                    Beskrivelse.DELTIDSJOBB_VIL_MER,
                    mapOf(
                        Pair("noekkel3", "verdi3"),
                        Pair("noekkel4", "verdi4")
                    )
                )
            )
        ),
        Annet(
            null
        )
    )
}

private infix fun TidspunktFraKildeResponse.shouldBeEqualTo(tidspunktFraKilde: TidspunktFraKilde): TidspunktFraKildeResponse {
    tidspunkt shouldBe tidspunktFraKilde.tidspunkt
    avviksType.name shouldBe tidspunktFraKilde.avviksType.name
    return this
}

private infix fun BrukerResponse.shouldBeEqualTo(bruker: Bruker): BrukerResponse {
    id shouldBe bruker.id
    type.name shouldBe bruker.type.name
    return this
}

private infix fun MetadataResponse.shouldBeEqualTo(metadata: Metadata): MetadataResponse {
    tidspunkt shouldBe metadata.tidspunkt
    utfoertAv shouldBeEqualTo metadata.utfoertAv
    kilde shouldBe metadata.kilde
    aarsak shouldBe metadata.aarsak
    tidspunktFraKilde?.shouldBeEqualTo(metadata.tidspunktFraKilde)
    return this
}

private infix fun UtdanningResponse.shouldBeEqualTo(utdanning: Utdanning): UtdanningResponse {
    nus shouldBe utdanning.nus
    bestaatt?.name shouldBe utdanning.bestaatt?.name
    godkjent?.name shouldBe utdanning.godkjent?.name
    return this
}

private infix fun HelseResponse.shouldBeEqualTo(helse: Helse): HelseResponse {
    helsetilstandHindrerArbeid.name shouldBe helse.helsetilstandHindrerArbeid.name
    return this
}

private infix fun AnnetResponse.shouldBeEqualTo(annet: Annet): AnnetResponse {
    andreForholdHindrerArbeid?.name shouldBe annet.andreForholdHindrerArbeid?.name
    return this
}

private infix fun OpplysningerOmArbeidssoekerResponse.shouldBeEqualTo(opplysninger: OpplysningerOmArbeidssoeker): OpplysningerOmArbeidssoekerResponse {
    opplysningerOmArbeidssoekerId shouldBe opplysninger.id
    periodeId shouldBe opplysninger.periodeId
    sendtInnAv shouldBeEqualTo opplysninger.sendtInnAv
    utdanning?.shouldBeEqualTo(opplysninger.utdanning)
    helse?.shouldBeEqualTo(opplysninger.helse)
    annet?.shouldBeEqualTo(opplysninger.annet)
    return this
}
