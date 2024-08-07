package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.createSamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.createSisteSamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getPidClaim
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getRequestBody
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
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer)

                    val response =
                        if (siste) {
                            arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer)
                        }

                    logger.info("Hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("/opplysninger-om-arbeidssoeker") {
                get {
                    val identitetsnummer = call.getPidClaim()
                    val siste = call.request.queryParameters["siste"]

                    val opplysninger = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(identitetsnummer)
                    val response =
                        if (siste != null && siste.toBoolean()) {
                            opplysninger.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            opplysninger
                        }

                    logger.info("Hentet opplysninger for bruker")

                    call.respond(HttpStatusCode.OK, response)
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
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    val profilering = profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer)

                    val response =
                        if (siste) {
                            profilering.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            profilering
                        }

                    logger.info("Hentet profilering for bruker")

                    call.respond(HttpStatusCode.OK, response)
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

                    val response =
                        if (siste) {
                            createSisteSamletInformasjonResponse(arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer), opplysningerOmArbeidssoekerService, profileringService)
                        } else {
                            createSamletInformasjonResponse(arbeidssoekerperiodeService.hentArbeidssoekerperioder(identitetsnummer), identitetsnummer.verdi, opplysningerOmArbeidssoekerService, profileringService)
                        }

                    logger.info("Hentet siste samlet informasjon for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
        authenticate("azure") {
            route("/veileder/arbeidssoekerperioder") {
                post {
                    val (identitetsnummer) = call.getRequestBody<ArbeidssoekerperiodeRequest>()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer))

                    val response =
                        if (siste) {
                            arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer))
                        }

                    logger.info("Veileder hentet arbeidssøkerperioder for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("/veileder/opplysninger-om-arbeidssoeker") {
                post {
                    val (identitetsnummer, periodeId) = call.getRequestBody<OpplysningerOmArbeidssoekerRequest>()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    val opplysninger =
                        when {
                            periodeId != null -> {
                                if (!call.verifyPeriodeId(periodeId, Identitetsnummer(identitetsnummer), arbeidssoekerperiodeService)) {
                                    return@post
                                }
                                opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periodeId)
                            }
                            else -> {
                                opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))
                            }
                        }

                    val response =
                        if (siste) {
                            opplysninger.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            opplysninger
                        }

                    logger.info("Veileder hentet opplysninger-om-arbeidssøker for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("/veileder/profilering") {
                post {
                    val (identitetsnummer, periodeId) = call.getRequestBody<ProfileringRequest>()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    val profilering =
                        when {
                            periodeId != null -> {
                                if (!call.verifyPeriodeId(periodeId, Identitetsnummer(identitetsnummer), arbeidssoekerperiodeService)) {
                                    return@post
                                }
                                profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periodeId)
                            }
                            else -> {
                                profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))
                            }
                        }

                    val response =
                        if (siste) {
                            profilering.maxByOrNull { it.sendtInnAv.tidspunkt }?.let { listOf(it) } ?: emptyList()
                        } else {
                            profilering
                        }

                    logger.info("Veileder hentet profilering for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("/veileder/samlet-informasjon") {
                post {
                    val (identitetsnummer) = call.getRequestBody<ArbeidssoekerperiodeRequest>()
                    val siste = call.request.queryParameters["siste"]?.toBoolean() ?: false

                    if (!call.verifyAccessFromToken(autorisasjonService, Identitetsnummer(identitetsnummer))) {
                        return@post
                    }

                    val arbeidssoekerperioder = arbeidssoekerperiodeService.hentArbeidssoekerperioder(Identitetsnummer(identitetsnummer))

                    val response =
                        if (siste) {
                            createSisteSamletInformasjonResponse(arbeidssoekerperioder, opplysningerOmArbeidssoekerService, profileringService)
                        } else {
                            createSamletInformasjonResponse(arbeidssoekerperioder, identitetsnummer, opplysningerOmArbeidssoekerService, profileringService)
                        }

                    logger.info("Veileder hentet siste samlet informasjon for bruker")

                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
