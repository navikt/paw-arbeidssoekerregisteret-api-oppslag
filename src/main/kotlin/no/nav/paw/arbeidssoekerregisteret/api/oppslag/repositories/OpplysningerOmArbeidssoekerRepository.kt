package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.AnnetTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ArbeidserfaringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseMedDetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.DetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.HelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.UtdanningTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.ArbeidserfaringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.BeskrivelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.BrukerTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.JaNeiVetIkkeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.domain.response.UtdanningResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerOmArbeidssoekerRepository(private val database: Database) {
    fun hentOpplysningerOmArbeidssoeker(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> =
        transaction(database) {
            val opplysningerOmArbeidssoekerIder =
                PeriodeOpplysningerTable.selectAll().where { PeriodeOpplysningerTable.periodeId eq periodeId }.mapNotNull { resultRow ->
                    resultRow[PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId]
                }
            opplysningerOmArbeidssoekerIder.mapNotNull { opplysningerOmArbeidssoekerId ->
                OpplysningerOmArbeidssoekerTable.selectAll().where {
                    OpplysningerOmArbeidssoekerTable.id eq opplysningerOmArbeidssoekerId
                }.singleOrNull()?.let {
                        resultRow ->
                    OpplysningerOmArbeidssoekerConverter().konverterTilOpplysningerOmArbeidssoekerResponse(resultRow, periodeId)
                }
            }
        }

    fun lagreOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) {
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 200

            val eksisterendeOpplysningerOmArbeidssoeker =
                OpplysningerOmArbeidssoekerTable
                    .selectAll().where { OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId eq opplysningerOmArbeidssoeker.id }
                    .singleOrNull()

            if (eksisterendeOpplysningerOmArbeidssoeker != null) {
                val eksisterendePeriodeOpplysninger =
                    PeriodeOpplysningerTable
                        .selectAll().where {
                            (PeriodeOpplysningerTable.periodeId eq opplysningerOmArbeidssoeker.periodeId) and
                                (PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId eq eksisterendeOpplysningerOmArbeidssoeker[OpplysningerOmArbeidssoekerTable.id].value)
                        }
                        .singleOrNull()

                if (eksisterendePeriodeOpplysninger == null) {
                    settInnPeriodeOpplysninger(
                        eksisterendeOpplysningerOmArbeidssoeker[OpplysningerOmArbeidssoekerTable.id].value,
                        opplysningerOmArbeidssoeker.periodeId
                    )
                }
            } else {
                settInnOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker)
            }
        }
    }

    fun hentPeriodeOpplysninger(periodeId: UUID) =
        transaction(database) {
            PeriodeOpplysningerTable.selectAll()
                .where { PeriodeOpplysningerTable.periodeId eq periodeId }
                .toList()
        }

    fun hentAllePeriodeOpplysninger() =
        transaction(database) {
            PeriodeOpplysningerTable.selectAll()
                .toList()
        }

    fun hentAlleOpplysningerOmArbeidssoeker() =
        transaction(database) {
            OpplysningerOmArbeidssoekerTable.selectAll()
                .toList()
        }

    private fun settInnOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) {
        val sendtInnAvId = ArbeidssoekerperiodeRepository(database).settInnMetadata(opplysningerOmArbeidssoeker.sendtInnAv)
        val utdanningId = settInnUtdanning(opplysningerOmArbeidssoeker.utdanning)
        val helseId = settInnHelse(opplysningerOmArbeidssoeker.helse)
        val arbeidserfaringId = settInnArbeidserfaring(opplysningerOmArbeidssoeker.arbeidserfaring)
        val annetId = settInnAnnet(opplysningerOmArbeidssoeker.annet)
        val opplysningerOmArbeidssoekerId =
            settInnOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoeker, sendtInnAvId, utdanningId, helseId, arbeidserfaringId, annetId)

        opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser.forEach { beskrivelseMedDetaljer ->
            val beskrivelseMedDetaljerId = settInnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId)
            val beskrivelserId = settInnBeskrivelse(beskrivelseMedDetaljer.beskrivelse, beskrivelseMedDetaljerId)
            beskrivelseMedDetaljer.detaljer.forEach { detalj ->
                settInnDetaljer(beskrivelserId, detalj)
            }
        }

        settInnPeriodeOpplysninger(opplysningerOmArbeidssoekerId, opplysningerOmArbeidssoeker.periodeId)
    }

    private fun settInnPeriodeOpplysninger(
        opplysningerOmArbeidssoekerId: Long,
        periodeId: UUID
    ) {
        PeriodeOpplysningerTable.insert {
            it[PeriodeOpplysningerTable.periodeId] = periodeId
            it[opplysningerOmArbeidssoekerTableId] = opplysningerOmArbeidssoekerId
        }
    }

    private fun settInnUtdanning(utdanning: Utdanning): Long =
        UtdanningTable.insertAndGetId {
            it[nus] = utdanning.nus
            it[bestaatt] = JaNeiVetIkke.valueOf(utdanning.bestaatt.name)
            it[godkjent] = JaNeiVetIkke.valueOf(utdanning.godkjent.name)
        }.value

    private fun settInnHelse(helse: Helse): Long =
        HelseTable.insertAndGetId {
            it[helsetilstandHindrerArbeid] = JaNeiVetIkke.valueOf(helse.helsetilstandHindrerArbeid.name)
        }.value

    private fun settInnArbeidserfaring(arbeidserfaring: Arbeidserfaring): Long =
        ArbeidserfaringTable.insertAndGetId {
            it[harHattArbeid] = JaNeiVetIkke.valueOf(arbeidserfaring.harHattArbeid.name)
        }.value

    private fun settInnAnnet(annet: Annet): Long =
        AnnetTable.insertAndGetId {
            it[andreForholdHindrerArbeid] = JaNeiVetIkke.valueOf(annet.andreForholdHindrerArbeid.name)
        }.value

    private fun settInnOpplysningerOmArbeidssoeker(
        opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker,
        sendtInnAvId: Long,
        utdanningId: Long,
        helseId: Long,
        arbeidserfaringId: Long,
        annetId: Long
    ): Long =
        OpplysningerOmArbeidssoekerTable.insertAndGetId {
            it[opplysningerOmArbeidssoekerId] = opplysningerOmArbeidssoeker.id
            it[OpplysningerOmArbeidssoekerTable.sendtInnAvId] = sendtInnAvId
            it[OpplysningerOmArbeidssoekerTable.utdanningId] = utdanningId
            it[OpplysningerOmArbeidssoekerTable.helseId] = helseId
            it[OpplysningerOmArbeidssoekerTable.arbeidserfaringId] = arbeidserfaringId
            it[OpplysningerOmArbeidssoekerTable.annetId] = annetId
        }.value

    private fun settInnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId: Long): Long =
        BeskrivelseMedDetaljerTable.insertAndGetId {
            it[BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId] = opplysningerOmArbeidssoekerId
        }.value

    private fun settInnBeskrivelse(
        beskrivelse: Beskrivelse,
        beskrivelseMedDetaljerId: Long
    ): Long =
        BeskrivelseTable.insertAndGetId {
            it[BeskrivelseTable.beskrivelse] = Beskrivelse.valueOf(beskrivelse.name)
            it[BeskrivelseTable.beskrivelseMedDetaljerId] = beskrivelseMedDetaljerId
        }.value

    private fun settInnDetaljer(
        beskrivelseId: Long,
        detalj: Map.Entry<String, String>
    ) {
        DetaljerTable.insert {
            it[DetaljerTable.beskrivelseId] = beskrivelseId
            it[noekkel] = detalj.key
            it[verdi] = detalj.value
        }
    }
}

class OpplysningerOmArbeidssoekerConverter {
    fun konverterTilOpplysningerOmArbeidssoekerResponse(
        resultRow: ResultRow,
        periodeId: UUID
    ): OpplysningerOmArbeidssoekerResponse {
        val situasjonIdPK = resultRow[OpplysningerOmArbeidssoekerTable.id]
        val opplysningerOmArbeidssoekerId = resultRow[OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId]
        val sendtInnAvId = resultRow[OpplysningerOmArbeidssoekerTable.sendtInnAvId]
        val utdanningId = resultRow[OpplysningerOmArbeidssoekerTable.utdanningId]

        val sendtInnAvMetadata = hentMetadataResponse(sendtInnAvId)
        val utdanning = hentUtdanningResponse(utdanningId)
        val helse = hentHelseResponse(resultRow[OpplysningerOmArbeidssoekerTable.helseId])
        val arbeidserfaring = hentArbeidserfaringResponse(resultRow[OpplysningerOmArbeidssoekerTable.arbeidserfaringId])
        val annet = hentAnnetResponse(resultRow[OpplysningerOmArbeidssoekerTable.annetId])
        val beskrivelseMedDetaljer = hentBeskrivelseMedDetaljerResponse(situasjonIdPK.value)

        return OpplysningerOmArbeidssoekerResponse(
            opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
            periodeId = periodeId,
            sendtInnAv = sendtInnAvMetadata,
            utdanning = utdanning,
            helse = helse,
            arbeidserfaring = arbeidserfaring,
            annet = annet,
            jobbsituasjon = beskrivelseMedDetaljer
        )
    }

    private fun hentMetadataResponse(metadataId: Long): MetadataResponse {
        return MetadataTable.selectAll().where { MetadataTable.id eq metadataId }
            .singleOrNull()?.let { metadataResultRow ->
                val utfoertAvId = metadataResultRow[MetadataTable.utfoertAvId]
                val bruker =
                    BrukerTable.selectAll().where { BrukerTable.id eq utfoertAvId }
                        .singleOrNull()?.let { brukerResultRow ->
                            BrukerResponse(
                                type = BrukerTypeResponse.valueOf(brukerResultRow[BrukerTable.type].name)
                            )
                        } ?: throw RuntimeException("Fant ikke bruker: $utfoertAvId")

                MetadataResponse(
                    tidspunkt = metadataResultRow[MetadataTable.tidspunkt],
                    utfoertAv = bruker,
                    kilde = metadataResultRow[MetadataTable.kilde],
                    aarsak = metadataResultRow[MetadataTable.aarsak]
                )
            } ?: throw RuntimeException("Fant ikke metadata $metadataId")
    }

    private fun hentUtdanningResponse(utdanningId: Long): UtdanningResponse {
        return UtdanningTable.selectAll().where { UtdanningTable.id eq utdanningId }
            .singleOrNull()?.let { utdanningResultRow ->
                UtdanningResponse(
                    nus = utdanningResultRow[UtdanningTable.nus],
                    bestaatt = JaNeiVetIkkeResponse.valueOf(utdanningResultRow[UtdanningTable.bestaatt].name),
                    godkjent = JaNeiVetIkkeResponse.valueOf(utdanningResultRow[UtdanningTable.godkjent].name)
                )
            } ?: throw RuntimeException("Fant ikke utdanning: $utdanningId")
    }

    private fun hentHelseResponse(helseId: Long): HelseResponse {
        return HelseTable.selectAll().where { HelseTable.id eq helseId }
            .singleOrNull()?.let { helseResultRow ->
                HelseResponse(
                    helseTilstandHindrerArbeid = JaNeiVetIkkeResponse.valueOf(helseResultRow[HelseTable.helsetilstandHindrerArbeid].name)
                )
            } ?: throw RuntimeException("Fant ikke helse: $helseId")
    }

    private fun hentArbeidserfaringResponse(arbeidserfaringId: Long): ArbeidserfaringResponse {
        return ArbeidserfaringTable.selectAll().where { ArbeidserfaringTable.id eq arbeidserfaringId }
            .singleOrNull()?.let { arbeidserfaringResultRow ->
                ArbeidserfaringResponse(
                    harHattArbeid = JaNeiVetIkkeResponse.valueOf(arbeidserfaringResultRow[ArbeidserfaringTable.harHattArbeid].name)
                )
            } ?: throw RuntimeException("Fant ikke arbeidserfaring: $arbeidserfaringId")
    }

    private fun hentAnnetResponse(annetId: Long): AnnetResponse {
        return AnnetTable.selectAll().where { AnnetTable.id eq annetId }
            .singleOrNull()?.let { annetResultRow ->
                AnnetResponse(
                    andreForholdHindrerArbeid = JaNeiVetIkkeResponse.valueOf(annetResultRow[AnnetTable.andreForholdHindrerArbeid].name)
                )
            } ?: throw RuntimeException("Fant ikke annet: $annetId")
    }

    private fun hentBeskrivelseMedDetaljerResponse(opplysningerOmArbeidssoekerId: Long): List<BeskrivelseMedDetaljerResponse> {
        return BeskrivelseMedDetaljerTable.selectAll().where {
            BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId eq opplysningerOmArbeidssoekerId
        }
            .map { beskrivelseMedDetaljer ->
                val beskrivelseMedDetaljerId = beskrivelseMedDetaljer[BeskrivelseMedDetaljerTable.id].value
                val beskrivelse = hentBeskrivelseResponse(beskrivelseMedDetaljerId)
                val detaljer = hentDetaljerResponse(beskrivelseMedDetaljerId)
                BeskrivelseMedDetaljerResponse(
                    beskrivelse = beskrivelse,
                    detaljer = detaljer
                )
            }
    }

    private fun hentBeskrivelseResponse(beskrivelseMedDetaljerId: Long): BeskrivelseResponse {
        return BeskrivelseTable.selectAll().where { BeskrivelseTable.beskrivelseMedDetaljerId eq beskrivelseMedDetaljerId }
            .singleOrNull()?.let { beskrivelse ->
                BeskrivelseResponse.valueOf(beskrivelse[BeskrivelseTable.beskrivelse].name)
            } ?: throw RuntimeException("Fant ikke beskrivelse: $beskrivelseMedDetaljerId")
    }

    private fun hentDetaljerResponse(beskrivelseId: Long): Map<String, String> {
        return DetaljerTable.selectAll().where { DetaljerTable.beskrivelseId eq beskrivelseId }
            .associate { detaljerResultRow ->
                detaljerResultRow[DetaljerTable.noekkel] to detaljerResultRow[DetaljerTable.verdi]
            }
    }
}