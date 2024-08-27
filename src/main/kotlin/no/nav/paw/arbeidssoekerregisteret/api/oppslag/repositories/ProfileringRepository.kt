package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnProfileringerForIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnProfileringerForPeriodeId
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.lagreProfilering
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {
    fun hentProfileringForArbeidssoekerMedPeriodeId(periodeId: UUID): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringerForPeriodeId(periodeId)
        }

    fun hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer: Identitetsnummer): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringerForIdentitetsnummer(identitetsnummer)
        }

    fun opprettProfileringForArbeidssoeker(profilering: Profilering) {
        transaction(database) {
            lagreProfilering(profilering)
        }
    }

    fun storeBatch(profileringer: Sequence<Profilering>) =
        transaction(database) {
            maxAttempts = 2
            minRetryDelay = 20
            profileringer.forEach { profilering ->
                lagreProfilering(profilering)
            }
        }
}
