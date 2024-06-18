package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.isPeriodeIdValid
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyAccessFromToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.verifyPeriodeId
import java.util.*

fun Route.oppslagRoutes(
    autorisasjonService: AutorisasjonService,
    arbeidssoekerperiodeService: ArbeidssoekerperiodeService,
    opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    profileringService: ProfileringService
) {
    route("/api/v1") {
        authenticate("tokenx") {
            route("/arbeidssoekerperioder") {
                get {
                    val identitetsnummer = call.getPidClaim()
                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer)

                    logger.info("Hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
                }
            }
            route("/opplysninger-om-arbeidssoeker") {
                get {
                    val identitetsnummer = call.getPidClaim()
                    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(identitetsnummer)

                    logger.info("Hentet opplysninger for bruker")

                    call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
                }
            }
            route("/opplysninger-om-arbeidssoeker/{periodeId}") {
                get {
                    val periodeId = UUID.fromString(call.parameters["periodeId"])
                    val identitetsnummer = call.getPidClaim()

                    if (!isPeriodeIdValid(periodeId, identitetsnummer, arbeidssoekerperiodeService)) return@get

                    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periodeId)

                    logger.info("Hentet opplysninger for bruker")

                    call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
                }
            }
            route("/profilering") {
                get {
                    val identitetsnummer = call.getPidClaim()
                    val profilering = profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer)

                    logger.info("Hentet profilering for bruker")

                    call.respond(HttpStatusCode.OK, profilering)
                }
            }
            route("/profilering/{periodeId}") {
                get {
                    val periodeId = UUID.fromString(call.parameters["periodeId"])
                    val identitetsnummer = call.getPidClaim()

                    if (!isPeriodeIdValid(periodeId, identitetsnummer, arbeidssoekerperiodeService)) return@get

                    val profilering = profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

                    logger.info("Hentet profilering for bruker")

                    call.respond(HttpStatusCode.OK, profilering)
                }
            }
            route("/samlet-informasjon") {
                get {
                    val identitetsnummer = call.getPidClaim()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (siste) {
                        val sistePeriode = arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer).maxByOrNull { it.startet.tidspunkt }
                        val sisteOpplysninger = sistePeriode?.let { opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(it.periodeId) }
                        val sisteProfilering = sistePeriode?.let { profileringService.hentProfileringForArbeidssoekerMedPeriodeId(it.periodeId) }

                        val response =
                            SamletInformasjonResponse(
                                arbeidssoekerperioder = listOfNotNull(sistePeriode),
                                opplysningerOmArbeidssoeker = sisteOpplysninger ?: emptyList(),
                                profilering = sisteProfilering ?: emptyList()
                            )

                        logger.info("Hentet siste samlet informasjon for bruker")

                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer)
                        val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(identitetsnummer)
                        val profilering = profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer)

                        val response =
                            SamletInformasjonResponse(
                                arbeidssoekerperioder = arbeidssoekerperioder,
                                opplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker,
                                profilering = profilering
                            )

                        logger.info("Hentet samlet informasjon for bruker")

                        call.respond(HttpStatusCode.OK, response)
                    }
                }
            }
        }
        authenticate("azure") {
            route("/veileder/arbeidssoekerperioder") {
                post {
                    val (identitetsnummer) = call.receive<ArbeidssoekerperiodeRequest>()

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer))

                    logger.info("Veileder hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, arbeidssoekerperioder)
                }
            }
            route("/veileder/opplysninger-om-arbeidssoeker") {
                post {
                    val (identitetsnummer, periodeId) = call.receive<OpplysningerOmArbeidssoekerRequest>()

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    if (!call.verifyPeriodeId(periodeId, Identitetsnummer(identitetsnummer), arbeidssoekerperiodeService)) {
                        return@post
                    }

                    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periodeId)

                    logger.info("Veileder hentet opplysninger-om-arbeidssøker for bruker")

                    call.respond(HttpStatusCode.OK, opplysningerOmArbeidssoeker)
                }
            }
            route("/veileder/profilering") {
                post {
                    val (identitetsnummer, periodeId) = call.receive<ProfileringRequest>()

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    if (!call.verifyPeriodeId(periodeId, Identitetsnummer(identitetsnummer), arbeidssoekerperiodeService)) {
                        return@post
                    }

                    val profilering = profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)

                    logger.info("Veileder hentet profilering for bruker")

                    call.respond(HttpStatusCode.OK, profilering)
                }
            }
            route("/veileder/samlet-informasjon") {
                post {
                    val (identitetsnummer) = call.receive<ArbeidssoekerperiodeRequest>()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    if (siste) {
                        val sistePeriode = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer)).maxByOrNull { it.startet.tidspunkt }
                        val sisteOpplysninger = sistePeriode?.let { opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(it.periodeId) }
                        val sisteProfilering = sistePeriode?.let { profileringService.hentProfileringForArbeidssoekerMedPeriodeId(it.periodeId) }

                        val response =
                            SamletInformasjonResponse(
                                arbeidssoekerperioder = listOfNotNull(sistePeriode),
                                opplysningerOmArbeidssoeker = sisteOpplysninger ?: emptyList(),
                                profilering = sisteProfilering ?: emptyList()
                            )

                        logger.info("Veileder hentet siste samlet informasjon for bruker")

                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer))
                        val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))
                        val profilering = profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))

                        val response =
                            SamletInformasjonResponse(
                                arbeidssoekerperioder = arbeidssoekerperioder,
                                opplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker,
                                profilering = profilering
                            )

                        logger.info("Veileder hentet samlet informasjon for bruker")

                        call.respond(HttpStatusCode.OK, response)
                    }
                }
            }
        }
    }
}
