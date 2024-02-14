package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PGEnum
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import org.jetbrains.exposed.dao.id.LongIdTable

object OpplysningerOmArbeidssoekerTable : LongIdTable("opplysninger_om_arbeidssoeker") {
    val opplysningerOmArbeidssoekerId = uuid("opplysninger_om_arbeidssoeker_id")
    val sendtInnAvId = long("sendt_inn_av_id").references(MetadataTable.utfoertAvId)
    val utdanningId = long("utdanning_id").references(UtdanningTable.id)
    val helseId = long("helse_id").references(HelseTable.id)
    val arbeidserfaringId = long("arbeidserfaring_id").references(ArbeidserfaringTable.id)
    val annetId = long("annet_id").references(AnnetTable.id)
}

object PeriodeOpplysningerTable : LongIdTable("periode_opplysninger") {
    val periodeId = uuid("periode_id")
    val opplysningerOmArbeidssoekerTableId =
        long("opplysninger_om_arbeidssoeker_id").references(
            OpplysningerOmArbeidssoekerTable.id
        )
}

object UtdanningTable : LongIdTable("utdanning") {
    val nus = varchar("nus", 255)
    val bestaatt =
        customEnumeration("bestaatt", "JaNeiVetIkke", { value -> JaNeiVetIkke.valueOf(value as String) }, { PGEnum("JaNeiVetIkke", it) })
    val godkjent =
        customEnumeration("godkjent", "JaNeiVetIkke", { value -> JaNeiVetIkke.valueOf(value as String) }, { PGEnum("JaNeiVetIkke", it) })
}

object HelseTable : LongIdTable("helse") {
    val helsetilstandHindrerArbeid =
        customEnumeration("helsetilstand_hindrer_arbeid", "JaNeiVetIkke", { value -> JaNeiVetIkke.valueOf(value as String) }, {
            PGEnum("JaNeiVetIkke", it)
        })
}

object ArbeidserfaringTable : LongIdTable("arbeidserfaring") {
    val harHattArbeid =
        customEnumeration(
            "har_hatt_arbeid",
            "JaNeiVetIkke",
            { value -> JaNeiVetIkke.valueOf(value as String) },
            { PGEnum("JaNeiVetIkke", it) }
        )
}

object AnnetTable : LongIdTable("annet") {
    val andreForholdHindrerArbeid =
        customEnumeration("andre_forhold_hindrer_arbeid", "JaNeiVetIkke", { value -> JaNeiVetIkke.valueOf(value as String) }, {
            PGEnum("JaNeiVetIkke", it)
        })
}

object BeskrivelseMedDetaljerTable : LongIdTable("beskrivelse_med_detaljer") {
    val opplysningerOmArbeidssoekerId =
        long("opplysninger_om_arbeidssoeker_id").references(
            OpplysningerOmArbeidssoekerTable.id
        )
}

object BeskrivelseTable : LongIdTable("beskrivelse") {
    val beskrivelse =
        customEnumeration(
            "beskrivelse",
            "Beskrivelse",
            { value -> Beskrivelse.valueOf(value as String) },
            { PGEnum("BeskrivelseEnum", it) }
        )
    val beskrivelseMedDetaljerId = long("beskrivelse_med_detaljer_id").references(BeskrivelseMedDetaljerTable.id)
}

object DetaljerTable : LongIdTable("detaljer") {
    val beskrivelseId = long("beskrivelse_id").references(BeskrivelseTable.id)
    val noekkel = varchar("noekkel", 50)
    val verdi = varchar("verdi", 255)
}
