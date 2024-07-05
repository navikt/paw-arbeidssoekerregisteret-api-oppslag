package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.Arbeidssoekerperioder
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.arbeidssoekerperioderObjectMapper
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getAzureM2MToken
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.getTokenXToken
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class OppslagRoutesTest : FreeSpec({
    val testPeriodeId = UUID.randomUUID()
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    "/arbeidssoekerperioder should respond with 200 OK" {
        val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(mockk<ArbeidssoekerperiodeRepository>(relaxed = true))

        every {
            arbeidssoekerperiodeService.hentArbeidssoekerperioder(any())
        } returns emptyList()

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val response = client.get("api/v1/arbeidssoekerperioder") { bearerAuth(getTokenXToken(oauth)) }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "/arbeidssoekerperioder?siste=true should respond with 200 OK and last arbeidssoekerperiode" {
        val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(mockk<ArbeidssoekerperiodeRepository>(relaxed = true))

        every {
            arbeidssoekerperiodeService.hentArbeidssoekerperioder(any())
        } returns getArbeidssoekerperiodeResponse(testPeriodeId)

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val response =
                client.get("api/v1/arbeidssoekerperioder?siste=true") {
                    bearerAuth(getTokenXToken(oauth))
                }
            response.status shouldBe HttpStatusCode.OK

            val responseBody = response.bodyAsText()
            val objectMapper = arbeidssoekerperioderObjectMapper()
            val responseList = objectMapper.readValue<Arbeidssoekerperioder>(responseBody)

            responseList.size shouldBe 1
            responseList[0].periodeId shouldBe testPeriodeId
        }
    }

    "/arbeidssoekerperioder should respond with 401 Unauthorized without token" {
        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val noPidInTokenResponse =
                client.get("api/v1/arbeidssoekerperioder")

            noPidInTokenResponse.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    "/opplysninger-om-arbeidssoeker/{periodeId} should return OK" {
        val opplysningerOmArbeidssoekerService = mockk<OpplysningerOmArbeidssoekerService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, opplysningerOmArbeidssoekerService, mockk(relaxed = true))
                }
            }

            val response = client.get("api/v1/opplysninger-om-arbeidssoeker/$testPeriodeId") { bearerAuth(getTokenXToken(oauth)) }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "/opplysninger-om-arbeidssoeker/{periodeId} should return 401 Unauthorized without token" {
        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val noTokenResponse =
                client.get("api/v1/opplysninger-om-arbeidssoeker/$testPeriodeId")

            noTokenResponse.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    "/profilering/{periodeId} should return OK" {
        val profileringService = mockk<ProfileringService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            profileringService.hentProfileringForArbeidssoekerMedPeriodeId(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, mockk(relaxed = true), profileringService)
                }
            }

            val response = client.get("api/v1/profilering/$testPeriodeId") { bearerAuth(getTokenXToken(oauth)) }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "/profilering/{periodeId} should return 401 Unauthorized without token" {
        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val noTokenResponse =
                client.get("api/v1/profilering/$testPeriodeId")

            noTokenResponse.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    "/veileder/arbeidssoekerperioder should return OK if Nav-Ident is present and poaotilgang returns true" {
        val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(mockk<ArbeidssoekerperiodeRepository>(relaxed = true))
        val autorisasjonService = mockk<AutorisasjonService>(relaxed = true)
        every {
            arbeidssoekerperiodeService.hentArbeidssoekerperioder(any())
        } returns emptyList()
        every {
            autorisasjonService.verifiserTilgangTilBruker(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(autorisasjonService, arbeidssoekerperiodeService, mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(getAzureM2MToken(oauth))
                    header("Nav-Ident", "test")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "/veileder/arbeidssoekerperioder should return 403 Forbidden if Nav-Ident is present and poaotilgang returns false" {
        val autorisasjonService = mockk<AutorisasjonService>(relaxed = true)
        every {
            autorisasjonService.verifiserTilgangTilBruker(any(), any())
        } returns false

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(autorisasjonService, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(getAzureM2MToken(oauth))
                    contentType(ContentType.Application.Json)
                    header("Nav-Ident", "test")
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    "/veileder/arbeidssoekerperioder should return OK if auth is m2m and no Nav-Ident is present" {
        val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(mockk<ArbeidssoekerperiodeRepository>(relaxed = true))
        val autorisasjonService = mockk<AutorisasjonService>(relaxed = true)
        every {
            arbeidssoekerperiodeService.hentArbeidssoekerperioder(any())
        } returns emptyList()
        every {
            autorisasjonService.verifiserTilgangTilBruker(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(autorisasjonService, arbeidssoekerperiodeService, mockk(relaxed = true), mockk(relaxed = true))
                }
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/arbeidssoekerperioder") {
                    bearerAuth(getAzureM2MToken(oauth))
                    contentType(ContentType.Application.Json)
                    setBody(
                        ArbeidssoekerperiodeRequest(
                            identitetsnummer = "12345678911"
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
    "/veileder/opplysninger-om-arbeidssoeker should return 403 Forbidden if periodeId does not exist for user" {
        val opplysningerOmArbeidssoekerService = mockk<OpplysningerOmArbeidssoekerService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns false

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, opplysningerOmArbeidssoekerService, mockk(relaxed = true))
                }
            }

            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(token.serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = "12345678901",
                            periodeId = testPeriodeId
                        )
                    )
                }

            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    "/veileder/opplysninger-om-arbeidssoeker should return 200 OK if periodeId exists for user" {
        val opplysningerOmArbeidssoekerService = mockk<OpplysningerOmArbeidssoekerService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, opplysningerOmArbeidssoekerService, mockk(relaxed = true))
                }
            }

            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/opplysninger-om-arbeidssoeker") {
                    bearerAuth(token.serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpplysningerOmArbeidssoekerRequest(
                            identitetsnummer = "12345678901",
                            periodeId = testPeriodeId
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
    "/veileder/profilering should return 403 Forbidden if periodeId does not exist for user" {
        val profileringService = mockk<ProfileringService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            profileringService.hentProfileringForArbeidssoekerMedPeriodeId(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns false

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, mockk(relaxed = true), profileringService)
                }
            }

            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/profilering") {
                    bearerAuth(token.serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = "12345678901",
                            periodeId = testPeriodeId
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }
    "/veileder/profilering should return 200 OK if periodeId exists for user" {
        val profileringService = mockk<ProfileringService>(relaxed = true)
        val arbeidssoekerperiodeService = mockk<ArbeidssoekerperiodeService>(relaxed = true)

        every {
            profileringService.hentProfileringForArbeidssoekerMedPeriodeId(any())
        } returns emptyList()

        every {
            arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(any(), any())
        } returns true

        testApplication {
            application {
                configureAuthentication(oauth)
                configureSerialization()
                configureHTTP()
                routing {
                    oppslagRoutes(mockk(relaxed = true), arbeidssoekerperiodeService, mockk(relaxed = true), profileringService)
                }
            }

            val tokenMap =
                mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "roles" to listOf("access_as_application")
                )
            val token =
                oauth.issueToken(
                    claims = tokenMap
                )

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val response =
                client.post("api/v1/veileder/profilering") {
                    bearerAuth(token.serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProfileringRequest(
                            identitetsnummer = "12345678901",
                            periodeId = testPeriodeId
                        )
                    )
                }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
