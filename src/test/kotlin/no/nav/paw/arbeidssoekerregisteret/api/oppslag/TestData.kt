package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.NavAnsatt
import java.util.*

object TestData {
    val foedselsnummer = Identitetsnummer("18908396568")
    val navAnsatt = NavAnsatt(UUID.randomUUID().toString(), "Z999999")
}
