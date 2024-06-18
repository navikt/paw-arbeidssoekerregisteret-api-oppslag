package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import java.util.*

class ProfileringService(private val profileringRepository: ProfileringRepository) {
    fun hentProfileringForArbeidssoekerMedPeriodeId(periodeId: UUID) = profileringRepository.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

    fun hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer: Identitetsnummer) = profileringRepository.hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer)

    fun lagreBatch(batch: Sequence<Profilering>) = profileringRepository.storeBatch(batch)
}
