package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.AnnetTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseMedDetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.HelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.TidspunktFraKildeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.UtdanningTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JaNeiVetIkke
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JobbSituasjonBeskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toMetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfilertTilResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toMetadata(): Metadata {
    val tidspunkt = get(MetadataTable.tidspunkt)
    val bruker = toBruker()
    val kilde = get(MetadataTable.kilde)
    val aarsak = get(MetadataTable.aarsak)
    val tidspunktFraKildeId = getOrNull(MetadataTable.tidspunktFraKildeId)
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toTidspunktFraKilde() }
    return Metadata(tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

private fun ResultRow.toBruker(): Bruker {
    val type = get(BrukerTable.type)
    val brukerId = get(BrukerTable.brukerId)
    return Bruker(type, brukerId)
}

private fun ResultRow.toTidspunktFraKilde(): TidspunktFraKilde {
    val tidspunkt = get(TidspunktFraKildeTable.tidspunkt)
    val avviksType = get(TidspunktFraKildeTable.avviksType)
    return TidspunktFraKilde(tidspunkt, avviksType)
}

fun ResultRow.toOpplysningerResponse(beskrivelseMedDetaljer: List<BeskrivelseMedDetaljerResponse>): OpplysningerOmArbeidssoekerResponse {
    val opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId)
    val utdanningId = getOrNull(OpplysningerOmArbeidssoekerTable.utdanningId)
    val helseId = getOrNull(OpplysningerOmArbeidssoekerTable.helseId)
    val annetId = getOrNull(OpplysningerOmArbeidssoekerTable.annetId)
    val periodeId = get(PeriodeOpplysningerTable.periodeId)

    val sendtInnAvMetadata = toMetadata().toMetadataResponse()
    val utdanning = utdanningId?.let { toUtdanningResponse() }
    val helse = helseId?.let { toHelseResponse() }
    val annet = annetId?.let { toAnnetResponse() }

    return OpplysningerOmArbeidssoekerResponse(
        opplysningerId,
        periodeId,
        sendtInnAvMetadata,
        beskrivelseMedDetaljer,
        utdanning,
        helse,
        annet
    )
}

private fun ResultRow.toUtdanningResponse(): UtdanningResponse {
    val nus = get(UtdanningTable.nus)
    val bestaatt = getOrNull(UtdanningTable.bestaatt)?.let { JaNeiVetIkke.valueOf(it.name) }
    val godkjent = getOrNull(UtdanningTable.godkjent)?.let { JaNeiVetIkke.valueOf(it.name) }
    return UtdanningResponse(nus, bestaatt, godkjent)
}

private fun ResultRow.toHelseResponse() =
    HelseResponse(
        JaNeiVetIkke.valueOf(get(HelseTable.helsetilstandHindrerArbeid).name)
    )

private fun ResultRow.toAnnetResponse() =
    AnnetResponse(
        getOrNull(AnnetTable.andreForholdHindrerArbeid)?.let { JaNeiVetIkke.valueOf(it.name) }
    )

fun ResultRow.toBeskrivelseMedDetaljerResponse(detaljer: Map<String, String>): BeskrivelseMedDetaljerResponse {
    val id = get(BeskrivelseMedDetaljerTable.id)
    val beskrivelse = toBeskrivelseResponse() ?: throw RuntimeException("Fant ikke beskrivelse: $id")
    return BeskrivelseMedDetaljerResponse(beskrivelse, detaljer)
}

private fun ResultRow.toBeskrivelseResponse(): JobbSituasjonBeskrivelse? {
    return getOrNull(BeskrivelseTable.beskrivelse)?.let { JobbSituasjonBeskrivelse.valueOf(it.name) }
}

fun ResultRow.toPeriode(): Periode {
    val periodeId = get(PeriodeTable.periodeId)
    val identitetsnummer = get(PeriodeTable.identitetsnummer)
    val avsluttetMetadataId = getOrNull(PeriodeTable.avsluttetId)
    val startetMetadata = toStartetMetadata()
    val avsluttetMetadata = avsluttetMetadataId?.let { toAvsluttetMetadata() }
    return Periode(periodeId, identitetsnummer, startetMetadata, avsluttetMetadata)
}

private fun ResultRow.toStartetMetadata(): Metadata {
    val tidspunkt = get(StartetMetadataAlias[MetadataTable.tidspunkt])
    val bruker = toStartetBruker()
    val kilde = get(StartetMetadataAlias[MetadataTable.kilde])
    val aarsak = get(StartetMetadataAlias[MetadataTable.aarsak])
    val tidspunktFraKildeId = getOrNull(StartetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toStartetTidspunktFraKilde() }
    return Metadata(tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

private fun ResultRow.toAvsluttetMetadata(): Metadata {
    val tidspunkt = get(AvsluttetMetadataAlias[MetadataTable.tidspunkt])
    val bruker = toAvsluttetBruker()
    val kilde = get(AvsluttetMetadataAlias[MetadataTable.kilde])
    val aarsak = get(AvsluttetMetadataAlias[MetadataTable.aarsak])
    val tidspunktFraKildeId = getOrNull(AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toAvsluttetTidspunktFraKilde() }
    return Metadata(tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

private fun ResultRow.toStartetBruker(): Bruker {
    val type = get(StartetBrukerAlias[BrukerTable.type])
    val brukerId = get(StartetBrukerAlias[BrukerTable.brukerId])
    return Bruker(type, brukerId)
}

private fun ResultRow.toAvsluttetBruker(): Bruker {
    val type = get(AvsluttetBrukerAlias[BrukerTable.type])
    val brukerId = get(AvsluttetBrukerAlias[BrukerTable.brukerId])
    return Bruker(type, brukerId)
}

private fun ResultRow.toStartetTidspunktFraKilde(): TidspunktFraKilde {
    val tidspunkt = get(StartetTidspunktAlias[TidspunktFraKildeTable.tidspunkt])
    val avviksType = get(StartetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    return TidspunktFraKilde(tidspunkt, avviksType)
}

private fun ResultRow.toAvsluttetTidspunktFraKilde(): TidspunktFraKilde {
    val tidspunkt = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.tidspunkt])
    val avviksType = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    return TidspunktFraKilde(tidspunkt, avviksType)
}

fun ResultRow.toProfileringResponse(): ProfileringResponse {
    val profileringId = get(ProfileringTable.profileringId)
    val periodeId = get(ProfileringTable.periodeId)
    val opplysningerOmArbeidssoekerId = get(ProfileringTable.opplysningerOmArbeidssoekerId)
    val profilertTil = get(ProfileringTable.profilertTil)
    val jobbetSammenhengendeSeksAvTolvSisteManeder = get(ProfileringTable.jobbetSammenhengendeSeksAvTolvSisteManeder)
    val alder = get(ProfileringTable.alder)
    val sendtInnAv = toMetadata().toMetadataResponse()
    return ProfileringResponse(
        profileringId,
        periodeId,
        opplysningerOmArbeidssoekerId,
        sendtInnAv,
        profilertTil.toProfilertTilResponse(),
        jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder
    )
}
