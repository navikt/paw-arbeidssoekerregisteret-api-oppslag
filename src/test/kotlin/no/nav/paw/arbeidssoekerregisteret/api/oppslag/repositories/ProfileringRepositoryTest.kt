package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class ProfileringRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var repository: ProfileringRepository
    val periodeId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb")
    val periodeId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fec")
    val opplysningerOmArbeidssoekerId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fed")
    val opplysningerOmArbeidssoekerId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fee")

    beforeEach {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        repository = ProfileringRepository(database)
        val opplysningerRepository = OpplysningerOmArbeidssoekerRepository(database)
        settInnTestPeriode(database, periodeId1)
        settInnTestPeriode(database, periodeId2)
        val opplysninger1 = lagTestOpplysningerOmArbeidssoeker(periodeId1, opplysningerOmArbeidssoekerId1)
        val opplysninger2 = lagTestOpplysningerOmArbeidssoeker(periodeId2, opplysningerOmArbeidssoekerId2)
        opplysningerRepository.lagreOpplysningerOmArbeidssoeker(opplysninger1)
        opplysningerRepository.lagreOpplysningerOmArbeidssoeker(opplysninger2)
    }

    afterEach {
        dataSource.connection.close()
    }

    "Opprett og hent ut en profilering" {
        val profilering = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.opprettProfileringForArbeidssoeker(profilering)

        val profileringResponser = repository.hentProfileringForArbeidssoekerMedPeriodeId(profilering.periodeId)

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Opprett og hent ut flere profileringer" {
        val profilering1 = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        val profilering2 = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId2)
        repository.opprettProfileringForArbeidssoeker(profilering1)
        repository.opprettProfileringForArbeidssoeker(profilering2)

        val profileringResponser = repository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId1)

        profileringResponser.size shouldBe 2
        val profileringResponse1 = profileringResponser[0]
        profileringResponse1 shouldBeEqualTo profilering1
        val profileringResponse2 = profileringResponser[1]
        profileringResponse2 shouldBeEqualTo profilering2
    }

    "Hent ut profilering med PeriodeId" {
        val profilering = lagTestProfilering(periodeId1, opplysningerOmArbeidssoekerId1)
        repository.opprettProfileringForArbeidssoeker(profilering)
        val profileringResponser = repository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId1)

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Hent ut ikke-eksisterende profilering" {
        val profileringResponser = repository.hentProfileringForArbeidssoekerMedPeriodeId(UUID.randomUUID())

        profileringResponser.size shouldBe 0
    }

    "Lagre profileringer med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val profilering1 = lagTestProfilering(periodeId, UUID.randomUUID())
        val profilering2 = lagTestProfilering(periodeId, UUID.randomUUID())
        val profilering3 = lagTestProfilering(periodeId, UUID.randomUUID())
        val profileringer = sequenceOf(profilering1, profilering2, profilering3)
        repository.storeBatch(profileringer)

        val lagredeProfileringer = repository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

        lagredeProfileringer.size shouldBeExactly 3
        val lagredeProfilering1 = lagredeProfileringer[0]
        val lagredeProfilering2 = lagredeProfileringer[1]
        val lagredeProfilering3 = lagredeProfileringer[2]
        lagredeProfilering1 shouldBeEqualTo profilering1
        lagredeProfilering2 shouldBeEqualTo profilering2
        lagredeProfilering3 shouldBeEqualTo profilering3
    }
})

fun lagTestProfilering(
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID
) = Profilering(
    UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb"),
    periodeId,
    opplysningerOmArbeidssoekerId,
    Metadata(
        Instant.now(),
        Bruker(BrukerType.SLUTTBRUKER, "01017012345"),
        "KILDE",
        "AARSAK",
        null
    ),
    ProfilertTil.UDEFINERT,
    true,
    30
)

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

private infix fun ProfileringResponse.shouldBeEqualTo(profilering: Profilering): ProfileringResponse {
    profileringId shouldBe profilering.id
    periodeId shouldBe profilering.periodeId
    opplysningerOmArbeidssoekerId shouldBe profilering.opplysningerOmArbeidssokerId
    sendtInnAv shouldBeEqualTo profilering.sendtInnAv
    jobbetSammenhengendeSeksAvTolvSisteManeder shouldBe profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
    alder shouldBe profilering.alder
    return this
}
