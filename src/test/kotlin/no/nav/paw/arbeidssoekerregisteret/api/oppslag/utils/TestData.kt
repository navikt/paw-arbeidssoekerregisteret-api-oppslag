package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AvviksTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.NavAnsatt
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object TestData {
    val foedselsnummer = Identitetsnummer("18908396568")
    val navAnsatt = NavAnsatt(UUID.randomUUID().toString(), "Z999999")
}

fun getArbeidssoekerperiodeResponse(sistePeriodeId: UUID): Arbeidssoekerperioder {
    return listOf(
        ArbeidssoekerperiodeResponse(
            periodeId = UUID.randomUUID(),
            startet =
                MetadataResponse(
                    tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                    utfoertAv =
                        BrukerResponse(
                            type = BrukerType.SLUTTBRUKER,
                            id = "12345678901"
                        ),
                    kilde = "arbeidssokerregisteret",
                    aarsak = "",
                    tidspunktFraKilde =
                        TidspunktFraKildeResponse(
                            tidspunkt = Instant.now().minus(2, ChronoUnit.DAYS),
                            avviksType = AvviksTypeResponse.RETTING
                        )
                )
        ),
        ArbeidssoekerperiodeResponse(
            periodeId = UUID.randomUUID(),
            startet =
                MetadataResponse(
                    tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                    utfoertAv =
                        BrukerResponse(
                            type = BrukerType.SLUTTBRUKER,
                            id = "12345678901"
                        ),
                    kilde = "arbeidssokerregisteret",
                    aarsak = "",
                    tidspunktFraKilde =
                        TidspunktFraKildeResponse(
                            tidspunkt = Instant.now().minus(1, ChronoUnit.DAYS),
                            avviksType = AvviksTypeResponse.RETTING
                        )
                )
        ),
        ArbeidssoekerperiodeResponse(
            periodeId = sistePeriodeId,
            startet =
                MetadataResponse(
                    tidspunkt = Instant.now(),
                    utfoertAv =
                        BrukerResponse(
                            type = BrukerType.SLUTTBRUKER,
                            id = "12345678901"
                        ),
                    kilde = "arbeidssokerregisteret",
                    aarsak = "",
                    tidspunktFraKilde =
                        TidspunktFraKildeResponse(
                            tidspunkt = Instant.now(),
                            avviksType = AvviksTypeResponse.RETTING
                        )
                )
        )
    )
}

typealias Arbeidssoekerperioder = List<ArbeidssoekerperiodeResponse>
