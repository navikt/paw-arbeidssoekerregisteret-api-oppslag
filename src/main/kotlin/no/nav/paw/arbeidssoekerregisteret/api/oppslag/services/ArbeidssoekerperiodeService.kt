package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

class ArbeidssoekerperiodeService(private val arbeidssoekerperiodeRepository: ArbeidssoekerperiodeRepository) {
    fun hentArbeidssoekerperioder(identitetsnummer: Identitetsnummer): List<ArbeidssoekerperiodeResponse> = arbeidssoekerperiodeRepository.hentArbeidssoekerperioder(identitetsnummer)

    /*fun opprettEllerOppdaterArbeidssoekerperiode(arbeidssoekerperiode: Periode) {
        val finnesArbeidssoekerperiode = arbeidssoekerperiodeRepository.finnesArbeidssoekerperiode(arbeidssoekerperiode.id)
        if (finnesArbeidssoekerperiode) {
            arbeidssoekerperiodeRepository.oppdaterArbeidssoekerperiode(arbeidssoekerperiode)
        } else {
            arbeidssoekerperiodeRepository.opprettArbeidssoekerperiode(arbeidssoekerperiode)
        }
    }*/

    fun lagreBatch(batch: List<Periode>) {
        arbeidssoekerperiodeRepository.beginTransaction()
        arbeidssoekerperiodeRepository.storeBatch(batch)
        arbeidssoekerperiodeRepository.commitTransaction()
    }

    fun rollbackTransaction() {
        arbeidssoekerperiodeRepository.rollbackTransaction()
    }
}
