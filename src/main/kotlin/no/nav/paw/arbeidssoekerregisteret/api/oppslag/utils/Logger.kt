package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.NavAnsatt
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.logger get() = LoggerFactory.getLogger(T::class.java.name)

inline val auditLogger get() = LoggerFactory.getLogger("AuditLogger")

fun auditLogMelding(
    identitetsnummer: Identitetsnummer,
    navAnsatt: NavAnsatt,
    melding: String
): String =
    CefMessage.builder()
        .applicationName("paw-arbeidssoekerregisteret-api-oppslag")
        .event(CefMessageEvent.ACCESS)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.navIdent)
        .destinationUserId(identitetsnummer.verdi)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
