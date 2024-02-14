package no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.request

import java.util.UUID

data class ProfileringRequest(
    val identitetsnummer: String,
    val periodeId: UUID
)
