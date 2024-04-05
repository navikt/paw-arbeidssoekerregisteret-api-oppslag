package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.request.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.request.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.request.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getNavAnsattFromToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import java.util.UUID

fun Route.arbeidssokerRoutes(
    autorisasjonService: AutorisasjonService,
    arbeidssoekerperiodeService: ArbeidssoekerperiodeService,
    opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    profileringService: ProfileringService
) {
    route("/api/v1") {
        authenticate("tokenx") {
            route("/arbeidssoekerperioder") {
                get {
                    logger.info("Henter arbeidssøkerperioder for bruker")

                    val foedselsnummer = call.getPidClaim()

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(foedselsnummer)

                    logger.info("Hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
                }
            }
            route("/opplysninger-om-arbeidssoeker/{periodeId}") {
                get {
                    val periodeId = UUID.fromString(call.parameters["periodeId"])

                    logger.info("Henter opplysninger-om-arbeidssøker for bruker med periodeId: $periodeId")

                    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periodeId)

                    logger.info("Hentet opplysninger-om-arbeidssøker for bruker med periodeId: $periodeId")

                    call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
                }
            }
            route("/profilering/{periodeId}") {
                get {
                    val periodeId = UUID.fromString(call.parameters["periodeId"])

                    logger.info("Henter profilering for bruker med periodeId: $periodeId")

                    val profilering = profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

                    logger.info("Hentet profilering for bruker med periodeId: $periodeId")

                    call.respond(HttpStatusCode.OK, profilering)
                }
            }
        }
        authenticate("azure") {
            route("/veileder/arbeidssoekerperioder") {
                post {
                    logger.info("Veileder henter arbeidssøkerperioder for bruker")

                    val (identitesnummer) = call.receive<ArbeidssoekerperiodeRequest>()
                    val navAnsatt = call.getNavAnsattFromToken()

                    logger.info("Sjekker om NAV-ansatt har tilgang til bruker")

                    val harNavAnsattTilgangTilBruker =
                        autorisasjonService.verifiserTilgangTilBruker(
                            navAnsatt,
                            Identitetsnummer(identitesnummer)
                        )

                    if (!harNavAnsattTilgangTilBruker) {
                        logger.warn("NAV-ansatt har ikke tilgang til bruker")
                        call.respondText(status = HttpStatusCode.Forbidden, text = HttpStatusCode.Forbidden.description)
                        return@post
                    }

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitesnummer))

                    logger.info("Veileder hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
                }
            }
            route("/veileder/opplysninger-om-arbeidssoeker") {
                post {
                    val (identitetsnummer, periodeId) = call.receive<OpplysningerOmArbeidssoekerRequest>()

                    logger.info("Veileder henter opplysninger-om-arbeidssøker for bruker med periodeId: $periodeId")

                    val navAnsatt = call.getNavAnsattFromToken()

                    logger.info("Sjekker om NAV-ansatt har tilgang til bruker")

                    val harNavAnsattTilgangTilBruker =
                        autorisasjonService.verifiserTilgangTilBruker(
                            navAnsatt,
                            Identitetsnummer(identitetsnummer)
                        )

                    if (!harNavAnsattTilgangTilBruker) {
                        logger.warn("NAV-ansatt har ikke tilgang til bruker")
                        call.respondText(status = HttpStatusCode.Forbidden, text = HttpStatusCode.Forbidden.description)
                        return@post
                    }

                    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periodeId)

                    logger.info("Veileder hentet opplysninger-om-arbeidssøker for bruker med periodeId: $periodeId")

                    call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
                }
            }
            route("/veileder/profilering") {
                post {
                    val (identitetsnummer, periodeId) = call.receive<ProfileringRequest>()

                    logger.info("Veileder henter profilering for bruker med periodeId: $periodeId")

                    val navAnsatt = call.getNavAnsattFromToken()

                    logger.info("Sjekker om NAV-ansatt har tilgang til bruker")

                    val harNavAnsattTilgangTilBruker =
                        autorisasjonService.verifiserTilgangTilBruker(
                            navAnsatt,
                            Identitetsnummer(identitetsnummer)
                        )

                    if (!harNavAnsattTilgangTilBruker) {
                        logger.warn("NAV-ansatt har ikke tilgang til bruker")
                        call.respondText(status = HttpStatusCode.Forbidden, text = HttpStatusCode.Forbidden.description)
                        return@post
                    }

                    val profilering = profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

                    logger.info("Veileder hentet profilering for bruker med periodeId: $periodeId")

                    call.respond(HttpStatusCode.OK, profilering)
                }
            }
        }
    }
}
