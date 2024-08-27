package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.AnnetTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseMedDetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.DetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.HelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.TidspunktFraKildeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.UtdanningTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.util.*

val StartetMetadataAlias = MetadataTable.alias("startet_metadata")
val AvsluttetMetadataAlias = MetadataTable.alias("avsluttet_metadata")
val StartetBrukerAlias = BrukerTable.alias("startet_bruker")
val AvsluttetBrukerAlias = BrukerTable.alias("avsluttet_bruker")
val StartetTidspunktAlias = TidspunktFraKildeTable.alias("startet_tidspunkt")
val AvsluttetTidspunktAlias = TidspunktFraKildeTable.alias("avsluttet_tidspunkt")

fun Transaction.lagreMetadata(metadata: Metadata): Long {
    return MetadataTable.insertAndGetId {
        it[utfoertAvId] = lagreBruker(metadata.utfoertAv)
        it[tidspunkt] = metadata.tidspunkt
        it[kilde] = metadata.kilde
        it[aarsak] = metadata.aarsak
        it[tidspunktFraKildeId] = metadata.tidspunktFraKilde?.let { tidspunkt -> lagreTidspunktFraKilde(tidspunkt) }
    }.value
}

private fun Transaction.lagreBruker(bruker: Bruker): Long {
    val result =
        BrukerTable.upsert(
            BrukerTable.type,
            BrukerTable.brukerId,
            where = { (BrukerTable.type eq bruker.type) and (BrukerTable.brukerId eq bruker.id) }
        ) {
            it[type] = bruker.type
            it[brukerId] = bruker.id
        }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Upsert-operasjon returnerte ingen resultat")
    return result[BrukerTable.id].value
}

private fun Transaction.lagreTidspunktFraKilde(tidspunktFraKilde: TidspunktFraKilde): Long {
    return TidspunktFraKildeTable.insertAndGetId {
        it[tidspunkt] = tidspunktFraKilde.tidspunkt
        it[avviksType] = tidspunktFraKilde.avviksType
    }.value
}

fun Transaction.finnOpplysninger(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> {
    return OpplysningerOmArbeidssoekerTable
        .join(MetadataTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(UtdanningTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.utdanningId, UtdanningTable.id)
        .join(HelseTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.helseId, HelseTable.id)
        .join(AnnetTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.annetId, AnnetTable.id)
        .join(PeriodeOpplysningerTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.id, PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId)
        .selectAll()
        .where { PeriodeOpplysningerTable.periodeId eq periodeId }
        .mapNotNull {
            val opplysningerOmArbeidssoekerId = it[OpplysningerOmArbeidssoekerTable.id].value
            val beskrivelseMedDetaljer = finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId)
            it.toOpplysningerResponse(beskrivelseMedDetaljer)
        }
}

fun Transaction.finnOpplysninger(identitetsnummer: Identitetsnummer): List<OpplysningerOmArbeidssoekerResponse> {
    return OpplysningerOmArbeidssoekerTable
        .join(MetadataTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(UtdanningTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.utdanningId, UtdanningTable.id)
        .join(HelseTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.helseId, HelseTable.id)
        .join(AnnetTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.annetId, AnnetTable.id)
        .join(PeriodeOpplysningerTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.id, PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId)
        .join(PeriodeTable, JoinType.LEFT, PeriodeOpplysningerTable.periodeId, PeriodeTable.periodeId)
        .selectAll()
        .where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }
        .mapNotNull {
            val opplysningerOmArbeidssoekerId = it[OpplysningerOmArbeidssoekerTable.id].value
            val beskrivelseMedDetaljer = finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId)
            it.toOpplysningerResponse(beskrivelseMedDetaljer)
        }
}

private fun Transaction.finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId: Long): List<BeskrivelseMedDetaljerResponse> {
    return BeskrivelseMedDetaljerTable
        .join(BeskrivelseTable, JoinType.LEFT, BeskrivelseMedDetaljerTable.id, BeskrivelseTable.beskrivelseMedDetaljerId)
        .selectAll()
        .where { BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId eq opplysningerOmArbeidssoekerId }
        .mapNotNull {
            val beskrivelseId = it[BeskrivelseTable.id].value
            val detaljer = hentDetaljer(beskrivelseId)
            it.toBeskrivelseMedDetaljerResponse(detaljer)
        }
}

private fun Transaction.hentDetaljer(beskrivelseId: Long): Map<String, String> {
    return DetaljerTable.selectAll().where { DetaljerTable.beskrivelseId eq beskrivelseId }
        .associate { detaljerResultRow ->
            detaljerResultRow[DetaljerTable.noekkel] to detaljerResultRow[DetaljerTable.verdi]
        }
}

fun Transaction.lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker): Long {
    val id =
        OpplysningerOmArbeidssoekerTable.insertAndGetId {
            it[opplysningerOmArbeidssoekerId] = opplysninger.id
            it[sendtInnAvId] = lagreMetadata(opplysninger.sendtInnAv)
            it[utdanningId] = opplysninger.utdanning?.let { data -> lagreUtdanning(data) }
            it[helseId] = opplysninger.helse?.let { data -> lagreHelse(data) }
            it[annetId] = opplysninger.annet?.let { data -> lagreAnnet(data) }
        }.value
    opplysninger.jobbsituasjon.beskrivelser.forEach { beskrivelseMedDetaljer ->
        val beskrivelseMedDetaljerId = lagreBeskrivelseMedDetaljer(id)
        val beskrivelserId = lagreBeskrivelse(beskrivelseMedDetaljer.beskrivelse, beskrivelseMedDetaljerId)
        beskrivelseMedDetaljer.detaljer.forEach { detalj ->
            lagreDetaljer(beskrivelserId, detalj)
        }
    }
    lagrePeriodeOpplysninger(id, opplysninger.periodeId)
    return id
}

private fun Transaction.lagreUtdanning(utdanning: Utdanning): Long =
    UtdanningTable.insertAndGetId {
        it[nus] = utdanning.nus
        it[bestaatt] = utdanning.bestaatt?.let { bestaatt -> JaNeiVetIkke.valueOf(bestaatt.name) }
        it[godkjent] = utdanning.godkjent?.let { godkjent -> JaNeiVetIkke.valueOf(godkjent.name) }
    }.value

private fun Transaction.lagreHelse(helse: Helse): Long =
    HelseTable.insertAndGetId {
        it[helsetilstandHindrerArbeid] = JaNeiVetIkke.valueOf(helse.helsetilstandHindrerArbeid.name)
    }.value

private fun Transaction.lagreAnnet(annet: Annet): Long =
    AnnetTable.insertAndGetId {
        it[andreForholdHindrerArbeid] = annet.andreForholdHindrerArbeid?.let { data -> JaNeiVetIkke.valueOf(data.name) }
    }.value

private fun Transaction.lagreBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId: Long): Long =
    BeskrivelseMedDetaljerTable.insertAndGetId {
        it[BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId] = opplysningerOmArbeidssoekerId
    }.value

private fun Transaction.lagreBeskrivelse(
    beskrivelse: Beskrivelse,
    beskrivelseMedDetaljerId: Long
): Long =
    BeskrivelseTable.insertAndGetId {
        it[BeskrivelseTable.beskrivelse] = Beskrivelse.valueOf(beskrivelse.name)
        it[BeskrivelseTable.beskrivelseMedDetaljerId] = beskrivelseMedDetaljerId
    }.value

private fun Transaction.lagreDetaljer(
    beskrivelseId: Long,
    detaljer: Map.Entry<String, String>
) {
    DetaljerTable.insert {
        it[DetaljerTable.beskrivelseId] = beskrivelseId
        it[noekkel] = detaljer.key
        it[verdi] = detaljer.value
    }
}

fun Transaction.lagrePeriodeOpplysninger(
    opplysningerOmArbeidssoekerId: Long,
    periodeId: UUID
) {
    PeriodeOpplysningerTable.insert {
        it[PeriodeOpplysningerTable.periodeId] = periodeId
        it[opplysningerOmArbeidssoekerTableId] = opplysningerOmArbeidssoekerId
    }
}

// ### PERIODE ###
fun Transaction.finnPeriode(periodeId: UUID): Periode? {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
        .join(StartetBrukerAlias, JoinType.LEFT, StartetMetadataAlias[MetadataTable.utfoertAvId], StartetBrukerAlias[BrukerTable.id])
        .join(AvsluttetBrukerAlias, JoinType.LEFT, AvsluttetMetadataAlias[MetadataTable.utfoertAvId], AvsluttetBrukerAlias[BrukerTable.id])
        .join(StartetTidspunktAlias, JoinType.LEFT, StartetMetadataAlias[MetadataTable.tidspunktFraKildeId], StartetTidspunktAlias[TidspunktFraKildeTable.id])
        .join(AvsluttetTidspunktAlias, JoinType.LEFT, AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId], AvsluttetTidspunktAlias[TidspunktFraKildeTable.id])
        .selectAll()
        .where { PeriodeTable.periodeId eq periodeId }.singleOrNull()?.toPeriode()
}

fun Transaction.finnPerioder(identitetsnummer: Identitetsnummer): List<Periode> {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
        .join(StartetBrukerAlias, JoinType.LEFT, StartetMetadataAlias[MetadataTable.utfoertAvId], StartetBrukerAlias[BrukerTable.id])
        .join(AvsluttetBrukerAlias, JoinType.LEFT, AvsluttetMetadataAlias[MetadataTable.utfoertAvId], AvsluttetBrukerAlias[BrukerTable.id])
        .join(StartetTidspunktAlias, JoinType.LEFT, StartetMetadataAlias[MetadataTable.tidspunktFraKildeId], StartetTidspunktAlias[TidspunktFraKildeTable.id])
        .join(AvsluttetTidspunktAlias, JoinType.LEFT, AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId], AvsluttetTidspunktAlias[TidspunktFraKildeTable.id])
        .selectAll()
        .where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }
        .map { it.toPeriode() }
}

fun Transaction.lagrePeriode(periode: Periode) {
    PeriodeTable.insert {
        it[periodeId] = periode.id
        it[identitetsnummer] = periode.identitetsnummer
        it[startetId] = lagreMetadata(periode.startet)
        it[avsluttetId] = periode.avsluttet?.let { metadata -> lagreMetadata(metadata) }
    }
}

fun Transaction.endrePeriode(periode: Periode) {
    val avsluttetMetadata = requireNotNull(periode.avsluttet) { "Avsluttet kan ikke være null ved oppdatering av periode" }

    PeriodeTable.update({ PeriodeTable.periodeId eq periode.id }) {
        it[avsluttetId] = lagreMetadata(avsluttetMetadata)
    }
}

// ### PROFILERING ###
fun Transaction.finnProfileringerForPeriodeId(periodeId: UUID): List<ProfileringResponse> {
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .selectAll().where { ProfileringTable.periodeId eq periodeId }.map { it.toProfileringResponse() }
}

fun Transaction.finnProfileringerForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<ProfileringResponse> {
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(PeriodeTable, JoinType.LEFT, ProfileringTable.periodeId, PeriodeTable.periodeId)
        .selectAll().where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }.map { it.toProfileringResponse() }
}

fun Transaction.lagreProfilering(profilering: Profilering) {
    ProfileringTable.insert {
        it[profileringId] = profilering.id
        it[periodeId] = profilering.periodeId
        it[opplysningerOmArbeidssoekerId] = profilering.opplysningerOmArbeidssokerId
        it[sendtInnAvId] = lagreMetadata(profilering.sendtInnAv)
        it[profilertTil] = profilering.profilertTil
        it[jobbetSammenhengendeSeksAvTolvSisteManeder] = profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
        it[alder] = profilering.alder
    }
}
