package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.getunleash.DefaultUnleash
import org.mockito.Mockito

fun getUnleashMock(): DefaultUnleash {
    val unleashMockClient = Mockito.mock(DefaultUnleash::class.java)
    Mockito.`when`(unleashMockClient.isEnabled("aktiver-kafka-konsumere")).thenReturn(true)
    return unleashMockClient
}
