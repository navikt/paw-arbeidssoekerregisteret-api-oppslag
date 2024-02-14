package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.auditLogMelding
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.auditLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.poao_tilgang.client.NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangCachedClient
) {
    fun verifiserTilgangTilBruker(
        navAnsatt: NavAnsatt,
        identitetsnummer: Identitetsnummer
    ): Boolean {
        val harNavAnsattTilgang =
            poaoTilgangHttpClient.evaluatePolicy(
                NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput(navAnsatt.navIdent, identitetsnummer.verdi)
            ).getOrThrow().isPermit

        if (!harNavAnsattTilgang) {
            logger.info("NAV-ansatt har ikke tilgang til bruker")
        } else {
            auditLogger.info(auditLogMelding(identitetsnummer, navAnsatt, "NAV ansatt har hentet informasjon om bruker"))
        }
        return harNavAnsattTilgang
    }
}

data class NavAnsatt(val azureId: String, val navIdent: String)
