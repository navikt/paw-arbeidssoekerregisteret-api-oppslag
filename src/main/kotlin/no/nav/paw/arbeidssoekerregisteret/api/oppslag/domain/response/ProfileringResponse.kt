package no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import java.util.UUID

enum class ProfilertTilResponse {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

data class ProfileringResponse(
    val profileringId: UUID,
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTilResponse,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean,
    val alder: Int
)

fun ProfilertTil.toProfilertTilResponse() =
    when (this) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfilertTilResponse.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTilResponse.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> ProfilertTilResponse.OPPGITT_HINDRINGER
        ProfilertTil.UDEFINERT -> ProfilertTilResponse.UDEFINERT
        ProfilertTil.UKJENT_VERDI -> ProfilertTilResponse.UKJENT_VERDI
    }
