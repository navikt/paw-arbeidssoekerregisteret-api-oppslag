package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class ArbeidssoekerperiodeRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var repository: ArbeidssoekerperiodeRepository

    beforeSpec {
        dataSource = initTestDatabase()
        val database = Database.connect(dataSource)
        repository = ArbeidssoekerperiodeRepository(database)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent en periode" {
        val periode = nyStartetPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe periode
    }

    "Hent en periode for et gitt identitetsnummer" {
        val periode = nyStartetPeriode(identitetsnummer = "31017098765")
        repository.opprettArbeidssoekerperiode(periode)
        val retrievedPerioder = repository.hentArbeidssoekerperioder(Identitetsnummer(periode.identitetsnummer))

        retrievedPerioder.size shouldBeExactly 1
        val retrievedPeriode = retrievedPerioder.first()
        retrievedPeriode shouldBeEqualTo periode
    }

    "Oppdater periode med avsluttet metadata" {
        val periode =
            nyAvsluttetPeriode(
                avsluttetMetadata =
                nyMetadata(
                    ident = "ARENA",
                    brukerType = BrukerType.SYSTEM,
                    tidspunkt = Instant.now()
                )
            )
        repository.opprettArbeidssoekerperiode(periode)
        val updatedPeriode =
            periode.copy(
                avsluttetMetadata =
                nyMetadata(
                    ident = "ARENA",
                    brukerType = BrukerType.SYSTEM,
                    tidspunkt = Instant.now()
                )
            )

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe updatedPeriode
    }

    "Oppdater periode uten avsluttet metadata" {
        val periode = nyStartetPeriode()
        repository.opprettArbeidssoekerperiode(periode)
        val updatedPeriode =
            periode.copy(
                avsluttetMetadata =
                nyMetadata(
                    ident = "ARENA",
                    brukerType = BrukerType.SYSTEM,
                    tidspunkt = Instant.now()
                )
            )
        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode shouldBe updatedPeriode
    }

    "Oppdatere periode med avsluttet lik null skal ikke være mulig" {
        val periode = nyAvsluttetPeriode()
        repository.opprettArbeidssoekerperiode(periode)
        val updatedPeriode = periode.copy(avsluttetMetadata = null)

        val exception =
            shouldThrow<IllegalArgumentException> {
                repository.oppdaterArbeidssoekerperiode(updatedPeriode)
            }

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        exception.message shouldBe "Avsluttet kan ikke være null ved oppdatering av periode"
        retrievedPeriode?.avsluttet shouldNotBe null
    }

    "Lagre startede perioder i batch" {
        val periode1 = nyStartetPeriode(identitetsnummer = "01017012345")
        val periode2 = nyStartetPeriode(identitetsnummer = "02017012345")
        val perioder = sequenceOf(periode1, periode2)
        repository.storeBatch(perioder)

        val lagretPeriode1 = repository.hentArbeidssoekerperiode(periode1.id)
        val lagretPeriode2 = repository.hentArbeidssoekerperiode(periode2.id)

        lagretPeriode1 shouldNotBe null
        lagretPeriode2 shouldNotBe null

        lagretPeriode1!! shouldBe periode1
        lagretPeriode2!! shouldBe periode2
    }

    "Lagre noen avsluttede perioder i batch" {
        val periode1 =
            nyStartetPeriode(
                identitetsnummer = "01017012345",
                startetMetadata =
                nyMetadata(
                    ident = "01017012345",
                    brukerType = BrukerType.SLUTTBRUKER,
                    tidspunkt = Instant.now().minus(Duration.ofDays(1))
                )
            )
        val periode2 =
            nyStartetPeriode(
                identitetsnummer = "02017012345",
                startetMetadata =
                nyMetadata(
                    ident = "02017012345",
                    brukerType = BrukerType.SLUTTBRUKER,
                    tidspunkt = Instant.now().minus(Duration.ofDays(2))
                )
            )
        val periode3 =
            nyStartetPeriode(
                identitetsnummer = "03017012345",
                startetMetadata =
                nyMetadata(
                    ident = "12345",
                    brukerType = BrukerType.VEILEDER,
                    tidspunkt = Instant.now().minus(Duration.ofDays(3))
                )
            )
        val periode4 = periode1.copy(
            avsluttetMetadata =
            nyMetadata(
                ident = "ARENA",
                brukerType = BrukerType.SYSTEM,
                tidspunkt = Instant.now()
            )
        )
        val periode5 = periode2.copy(
            avsluttetMetadata =
            nyMetadata(
                ident = "ARENA",
                brukerType = BrukerType.SYSTEM,
                tidspunkt = Instant.now()
            )
        )
        val startedePerioder = sequenceOf(periode1, periode2, periode3)
        val avsluttedePerioder = sequenceOf(periode4, periode5)
        repository.storeBatch(startedePerioder)
        repository.storeBatch(avsluttedePerioder)

        val lagretPeriode1 = repository.hentArbeidssoekerperiode(periode1.id)
        val lagretPeriode2 = repository.hentArbeidssoekerperiode(periode2.id)
        val lagretPeriode3 = repository.hentArbeidssoekerperiode(periode3.id)
        val lagretPeriode4 = repository.hentArbeidssoekerperiode(periode4.id)
        val lagretPeriode5 = repository.hentArbeidssoekerperiode(periode5.id)

        lagretPeriode1 shouldNotBe null
        lagretPeriode2 shouldNotBe null
        lagretPeriode3 shouldNotBe null
        lagretPeriode4 shouldNotBe null
        lagretPeriode5 shouldNotBe null

        lagretPeriode1!! shouldBe periode4
        lagretPeriode2!! shouldBe periode5
        lagretPeriode3!! shouldBe periode3
        lagretPeriode4!! shouldBe periode4
        lagretPeriode5!! shouldBe periode5
        lagretPeriode1 shouldBe lagretPeriode4
        lagretPeriode2 shouldBe lagretPeriode5
    }
})

fun nyStartetPeriode(
    identitetsnummer: String = "01017012345",
    periodeId: UUID = UUID.randomUUID(),
    startetMetadata: Metadata = nyMetadata(
        ident = identitetsnummer,
        tidspunkt = Instant.now().minus(Duration.ofDays(30))
    ),
    avsluttetMetadata: Metadata? = null
) = Periode(
    periodeId,
    identitetsnummer,
    startetMetadata,
    avsluttetMetadata
)

fun nyAvsluttetPeriode(
    identitetsnummer: String = "01017012345",
    periodeId: UUID = UUID.randomUUID(),
    startetMetadata: Metadata = nyMetadata(
        ident = identitetsnummer,
        tidspunkt = Instant.now().minus(Duration.ofDays(30))
    ),
    avsluttetMetadata: Metadata = nyMetadata(
        ident = "ARENA",
        brukerType = BrukerType.SYSTEM,
        tidspunkt = Instant.now()
    )
) = Periode(
    periodeId,
    identitetsnummer,
    startetMetadata,
    avsluttetMetadata
)

fun nyMetadata(
    ident: String,
    brukerType: BrukerType = BrukerType.SLUTTBRUKER,
    tidspunkt: Instant
) = Metadata(
    tidspunkt,
    Bruker(brukerType, ident),
    "KILDE",
    "AARSAK",
    TidspunktFraKilde(
        tidspunkt.minusSeconds(60),
        AvviksType.UKJENT_VERDI
    )
)

private fun Periode.copy(avsluttetMetadata: Metadata?): Periode {
    return Periode(
        id,
        identitetsnummer,
        startet,
        avsluttetMetadata
    )
}

private infix fun TidspunktFraKildeResponse.shouldBeEqualTo(tidspunktFraKilde: TidspunktFraKilde): TidspunktFraKildeResponse {
    tidspunktFraKilde shouldNotBe null
    tidspunkt shouldBe tidspunktFraKilde.tidspunkt
    avviksType.name shouldBe tidspunktFraKilde.avviksType.name
    return this
}

private infix fun BrukerResponse.shouldBeEqualTo(bruker: Bruker): BrukerResponse {
    bruker shouldNotBe null
    id shouldBe bruker.id
    type.name shouldBe bruker.type.name
    return this
}

private infix fun MetadataResponse.shouldBeEqualTo(metadata: Metadata): MetadataResponse {
    metadata shouldNotBe null
    tidspunkt shouldBe metadata.tidspunkt
    utfoertAv shouldBeEqualTo metadata.utfoertAv
    kilde shouldBe metadata.kilde
    aarsak shouldBe metadata.aarsak
    tidspunktFraKilde?.shouldBeEqualTo(metadata.tidspunktFraKilde)
    return this
}

private infix fun ArbeidssoekerperiodeResponse.shouldBeEqualTo(periode: Periode): ArbeidssoekerperiodeResponse {
    periode shouldNotBe null
    periodeId shouldBe periode.id
    startet shouldBeEqualTo periode.startet
    avsluttet?.shouldBeEqualTo(periode.avsluttet)
    return this
}
