package no.nav.paw.arbeidssoekerregisteret.api.oppslag.models

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType as RestBrukerType

fun Metadata.toMetadataResponse() = MetadataResponse(this.tidspunkt, this.utfoertAv.toBrukerResponse(), this.kilde, this.aarsak)

fun Bruker.toBrukerResponse() = BrukerResponse(this.type.toBrukerTypeResponse())

fun BrukerType.toBrukerTypeResponse() =
    when (this) {
        BrukerType.VEILEDER -> RestBrukerType.VEILEDER
        BrukerType.SLUTTBRUKER -> RestBrukerType.SLUTTBRUKER
        BrukerType.SYSTEM -> RestBrukerType.SYSTEM
        BrukerType.UDEFINERT -> RestBrukerType.UDEFINERT
        BrukerType.UKJENT_VERDI -> RestBrukerType.UKJENT_VERDI
    }

fun ProfilertTil.toProfilertTilResponse() =
    when (this) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
        ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
        ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
    }
