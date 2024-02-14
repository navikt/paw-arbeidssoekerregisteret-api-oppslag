package no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.request

import java.util.UUID

data class OpplysningerOmArbeidssoekerRequest(
    val identitetsnummer: String,
    val periodeId: UUID
)
