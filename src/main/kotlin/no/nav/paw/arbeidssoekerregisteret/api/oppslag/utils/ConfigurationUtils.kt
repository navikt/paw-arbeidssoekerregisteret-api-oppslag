package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.Config
import java.lang.System.getenv

inline fun <reified T : Any> loadConfiguration(): T =
    ConfigLoaderBuilder.default()
        .apply {
            when (getenv("NAIS_CLUSTER_NAME")) {
                "prod-gcp", "dev-gcp" -> {
                    addResourceSource("/application-nais.yaml", true)
                }

                else -> {
                    addResourceSource("/application-local.yaml", true)
                }
            }
        }
        .strict()
        .build()
        .loadConfigOrThrow()

fun loadLocalConfiguration(): Config =
    ConfigLoaderBuilder.default()
        .addResourceSource("/application-local.yaml", true)
        .strict()
        .build()
        .loadConfigOrThrow()
