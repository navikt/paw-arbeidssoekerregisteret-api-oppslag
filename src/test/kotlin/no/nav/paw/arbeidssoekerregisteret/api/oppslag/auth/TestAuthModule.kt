package no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth

import io.ktor.server.application.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.loadConfiguration
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.configureAuthentication(oAuth2Server: MockOAuth2Server) {
    val config = loadConfiguration<Config>()
    val authProviders =
        config.authProviders.map {
            it.copy(
                discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
                clientId = "default"
            )
        }
    configureAuthentication(authProviders)
}
