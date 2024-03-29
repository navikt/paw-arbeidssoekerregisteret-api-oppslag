package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
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
