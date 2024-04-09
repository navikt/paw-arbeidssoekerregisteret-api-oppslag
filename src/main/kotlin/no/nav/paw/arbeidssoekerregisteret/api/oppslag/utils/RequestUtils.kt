package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.StatusException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.NavAnsatt
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun ApplicationCall.getClaim(
    issuer: String,
    name: String
): String? =
    authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims(issuer)
        ?.getStringClaim(name)

fun ApplicationCall.getPidClaim(): Identitetsnummer =
    getClaim("tokenx", "pid")?.toIdentitetsnummer()
        ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'pid'-claim i token fra tokenx-issuer")

private fun ApplicationCall.getNavAnsattAzureId(): String = getClaim("azure", "oid") ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'oid'-claim i token fra azure-issuer")

private fun ApplicationCall.getNavAnsattIdent(): String = getClaim("azure", "NAVident") ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'NAVident'-claim i token fra azure-issuer")

fun ApplicationCall.getNavAnsatt(): NavAnsatt =
    NavAnsatt(
        getNavAnsattAzureId(),
        getNavAnsattIdent()
    )

fun ApplicationCall.getNavAnsattWithNavIdentFromHeader(navIdent: String): NavAnsatt =
    NavAnsatt(
        getNavAnsattAzureId(),
        navIdent
    )

fun ApplicationCall.isMachineToMachineToken(): Boolean? =
    authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("azure")
        ?.getAsList("roles")
        ?.contains("access_as_application")

fun ApplicationCall.getNavAnsattFromToken(): NavAnsatt =
    if (this.isMachineToMachineToken() == true) {
        val navIdentFraHeader = this.request.headers["Nav-Ident"] ?: throw RuntimeException("Nav-Ident mangler i header")
        this.getNavAnsattWithNavIdentFromHeader(navIdentFraHeader)
    } else {
        this.getNavAnsatt()
    }
