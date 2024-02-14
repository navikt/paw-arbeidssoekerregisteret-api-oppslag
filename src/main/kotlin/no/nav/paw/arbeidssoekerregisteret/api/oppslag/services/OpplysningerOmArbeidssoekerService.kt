package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import java.util.*

class OpplysningerOmArbeidssoekerService(private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository) {
    fun hentOpplysningerOmArbeidssoeker(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> = opplysningerOmArbeidssoekerRepository.hentOpplysningerOmArbeidssoeker(periodeId)

    fun opprettOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) =
        opplysningerOmArbeidssoekerRepository.lagreOpplysningerOmArbeidssoeker(
            opplysningerOmArbeidssoeker
        )
}
