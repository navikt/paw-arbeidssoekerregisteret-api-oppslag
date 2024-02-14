package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(zoneId: ZoneId = ZoneId.of("Europe/Oslo")): LocalDateTime = LocalDateTime.ofInstant(this, zoneId)
