package no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import java.time.Instant
import java.util.*

data class ArbeidssoekerperiodeResponse(
    val periodeId: UUID,
    val startet: MetadataResponse,
    val avsluttet: MetadataResponse? = null
)

data class MetadataResponse(
    val tidspunkt: Instant,
    val utfoertAv: BrukerResponse,
    val kilde: String,
    val aarsak: String
)

data class BrukerResponse(
    val type: BrukerTypeResponse
)

enum class BrukerTypeResponse {
    UKJENT_VERDI,
    UDEFINERT,
    VEILEDER,
    SYSTEM,
    SLUTTBRUKER
}

fun Metadata.toMetadataResponse() = MetadataResponse(this.tidspunkt, this.utfoertAv.toBrukerResponse(), this.kilde, this.aarsak)

fun Bruker.toBrukerResponse() = BrukerResponse(this.type.toBrukerTypeResponse())

fun BrukerType.toBrukerTypeResponse() =
    when (this) {
        BrukerType.VEILEDER -> BrukerTypeResponse.VEILEDER
        BrukerType.SLUTTBRUKER -> BrukerTypeResponse.SLUTTBRUKER
        BrukerType.SYSTEM -> BrukerTypeResponse.SYSTEM
        BrukerType.UDEFINERT -> BrukerTypeResponse.UDEFINERT
        BrukerType.UKJENT_VERDI -> BrukerTypeResponse.UKJENT_VERDI
    }
