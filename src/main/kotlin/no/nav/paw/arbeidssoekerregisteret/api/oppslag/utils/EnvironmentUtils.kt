package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import java.lang.System.getenv

fun isLocalEnvironment() = getenv("NAIS_CLUSTER_NAME") == null
