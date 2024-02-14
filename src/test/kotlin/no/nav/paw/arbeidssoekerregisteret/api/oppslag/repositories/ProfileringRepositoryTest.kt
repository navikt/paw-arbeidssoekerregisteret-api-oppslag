package no.nav.paw.arbeidssokerregisteret.api.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.initTestDatabase
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class ProfileringRepositoryTest : StringSpec({
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
        settInnTestOpplysningerOmArbeidssoeker(database, periodeId1, opplysningerOmArbeidssoekerId1)
        settInnTestOpplysningerOmArbeidssoeker(database, periodeId2, opplysningerOmArbeidssoekerId2)
    }

    afterEach {
        dataSource.connection.close()
    }

    "Opprett og hent ut en profilering" {
        val repository = ProfileringRepository(database)
        val profilering = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.opprettProfileringForArbeidssoeker(profilering)

        val retrievedProfilering = repository.hentProfileringForArbeidssoekerMedPeriodeId(profilering.periodeId)

        retrievedProfilering.size shouldBe 1
    }

    "Opprett og hent ut flere profileringer" {
        val repository = ProfileringRepository(database)
        val profilering1 = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        val profilering2 = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId2)
        repository.opprettProfileringForArbeidssoeker(profilering1)
        repository.opprettProfileringForArbeidssoeker(profilering2)

        val retrievedProfilering = repository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId1)

        retrievedProfilering.size shouldBe 2
    }

    "Hent ut profilering med PeriodeId" {
        val repository = ProfileringRepository(database)
        val profilering = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.opprettProfileringForArbeidssoeker(profilering)
        val retrievedProfilering = repository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId1)

        retrievedProfilering.size shouldBe 1
    }

    "Hent ut ikke-eksisterende profilering" {
        val repository = ProfileringRepository(database)

        val retrievedProfilering = repository.hentProfileringForArbeidssoekerMedPeriodeId(UUID.randomUUID())

        retrievedProfilering.size shouldBe 0
    }
})

fun settInnTestOpplysningerOmArbeidssoeker(
    database: Database,
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
) {
    val opplysninger = hentTestOpplysningerOmArbeidssoeker(periodeId, opplysningerOmArbeidssoekerId)
    OpplysningerOmArbeidssoekerRepository(database).lagreOpplysningerOmArbeidssoeker(opplysninger)
}

fun lagTestProfilering(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
) = Profilering(
    UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb"),
    periodeId,
    opplysningerOmArbeidssoekerId,
    Metadata(
        Instant.now(),
        Bruker(BrukerType.SLUTTBRUKER, "1"),
        "KILDE",
        "AARSAK"
    ),
    ProfilertTil.UDEFINERT,
    true,
    30
)
