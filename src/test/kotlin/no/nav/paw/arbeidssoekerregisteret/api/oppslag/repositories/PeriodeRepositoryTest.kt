package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class PeriodeRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database

    beforeSpec {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent en periode" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe periode
    }

    "Hent en periode for et gitt identitetsnummer" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val identitetsnummer = Identitetsnummer("12345678911")

        val perioder = repository.hentArbeidssoekerperioder(identitetsnummer)

        perioder.size shouldBeExactly 1
    }

    "Oppdater periode med avsluttet metadata" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val updatedMetadata =
            Metadata(
                Instant.now(),
                Bruker(BrukerType.SYSTEM, "2"),
                "NY_KILDE",
                "NY_AARSAK"
            )

        val updatedPeriode = periode.copy(avsluttet = updatedMetadata)

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe updatedPeriode
    }

    "Oppdater periode uten avsluttet metadata" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode().copy(avsluttet = null)
        repository.opprettArbeidssoekerperiode(periode)

        val updatedMetadata =
            Metadata(
                Instant.now(),
                Bruker(BrukerType.SYSTEM, "2"),
                "NY_KILDE",
                "NY_AARSAK"
            )
        val updatedPeriode = periode.copy(avsluttet = updatedMetadata)

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode shouldBe updatedPeriode
    }

    "Oppdatere periode med avsluttet metadata til null skal ikke være mulig" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val updatedPeriode = periode.copy(avsluttet = null)

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode shouldNotBe updatedPeriode
    }
})

fun hentTestPeriode(periodeId: UUID? = null): Periode {
    val startetMetadata =
        Metadata(
            Instant.now(),
            Bruker(BrukerType.SLUTTBRUKER, "1"),
            "KILDE",
            "AARSAK"
        )
    val avsluttetMetadata =
        Metadata(
            Instant.now().plusMillis(100),
            Bruker(BrukerType.SYSTEM, "2"),
            "KILDE AVSLUTTET",
            "AARSAK AVSLUTTET"
        )
    return Periode(
        periodeId ?: UUID.randomUUID(),
        "12345678911",
        startetMetadata,
        avsluttetMetadata
    )
}

fun Periode.copy(avsluttet: Metadata?): Periode {
    return Periode(
        id,
        identitetsnummer,
        startet,
        avsluttet
    )
}
