package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.TidspunktFraKildeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toMetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.logger
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException
import java.util.*
import kotlin.sequences.Sequence
import kotlin.sequences.forEach

class ArbeidssoekerperiodeRepository(private val database: Database) {
    fun storeBatch(arbeidssoekerperioder: Sequence<Periode>) {
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 20
            arbeidssoekerperioder.forEach { periode ->
                if (finnesArbeidssoekerperiode(periode.id)) {
                    oppdaterArbeidssoekerperiode(periode)
                } else {
                    opprettArbeidssoekerperiode(periode)
                }
            }
        }
    }

    fun finnesArbeidssoekerperiode(periodeId: UUID): Boolean =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.periodeId eq periodeId }.singleOrNull() != null
        }

    fun hentArbeidssoekerperiode(periodeId: UUID): Periode? =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.periodeId eq periodeId }.singleOrNull()?.let { resultRow ->
                PeriodeConverter(this@ArbeidssoekerperiodeRepository).konverterResultRowToPeriode(resultRow)
            }
        }

    fun hentArbeidssoekerperioder(identitetsnummer: Identitetsnummer): List<ArbeidssoekerperiodeResponse> =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }.map { resultRow ->
                val periodeId = resultRow[PeriodeTable.periodeId]
                val startetId = resultRow[PeriodeTable.startetId]
                val avsluttetId = resultRow[PeriodeTable.avsluttetId]

                val startetMetadata = hentMetadata(startetId) ?: throw Error("Fant ikke startet metadata")
                val avsluttetMetadata = avsluttetId?.let { hentMetadata(it) }

                ArbeidssoekerperiodeResponse(periodeId, startetMetadata.toMetadataResponse(), avsluttetMetadata?.toMetadataResponse())
            }
        }

    fun hentMetadata(id: Long): Metadata? {
        return MetadataTable.selectAll().where { MetadataTable.id eq id }.singleOrNull()?.let { metadata ->
            val brukerId = metadata[MetadataTable.utfoertAvId]
            val bruker = hentBruker(brukerId)
            val tidspunktFraKildeId = metadata[MetadataTable.tidspunktFraKildeId]
            val tidspunktFraKilde = tidspunktFraKildeId?.let { hentTidspunktFraKilde(it) }
            Metadata(
                metadata[MetadataTable.tidspunkt],
                bruker,
                metadata[MetadataTable.kilde],
                metadata[MetadataTable.aarsak],
                tidspunktFraKilde
            )
        }
    }

    fun hentAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun hentTidspunktFraKilde(tidspunktFraKildeId: Long): TidspunktFraKilde? {
        return TidspunktFraKildeTable.selectAll().where { TidspunktFraKildeTable.id eq tidspunktFraKildeId }.singleOrNull()?.let {
            TidspunktFraKilde(
                it[TidspunktFraKildeTable.tidspunkt],
                it[TidspunktFraKildeTable.avviksType]
            )
        }
    }

    private fun hentBruker(brukerId: Long): Bruker? {
        return BrukerTable.selectAll().where { BrukerTable.id eq brukerId }.singleOrNull()?.let {
            Bruker(
                it[BrukerTable.type],
                it[BrukerTable.brukerId]
            )
        }
    }

    fun opprettArbeidssoekerperiode(arbeidssoekerperiode: Periode) {
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 20
            val startetId = settInnMetadata(arbeidssoekerperiode.startet)
            val avsluttetId = arbeidssoekerperiode.avsluttet?.let { settInnMetadata(it) }

            settInnArbeidssoekerperiode(arbeidssoekerperiode.id, arbeidssoekerperiode.identitetsnummer, startetId, avsluttetId)
        }
    }

    fun settInnMetadata(metadata: Metadata): Long {
        return MetadataTable.insertAndGetId {
            it[utfoertAvId] = settInnBruker(metadata.utfoertAv)
            it[tidspunkt] = metadata.tidspunkt
            it[kilde] = metadata.kilde
            it[aarsak] = metadata.aarsak
            it[tidspunktFraKildeId] = metadata.tidspunktFraKilde?.let { tidspunkt -> settInnTidspunktFraKilde(tidspunkt) }
        }.value
    }

    private fun settInnTidspunktFraKilde(tidspunktFraKilde: TidspunktFraKilde): Long {
        return TidspunktFraKildeTable.insertAndGetId {
            it[tidspunkt] = tidspunktFraKilde.tidspunkt
            it[avviksType] = tidspunktFraKilde.avviksType
        }.value
    }

    private fun settInnBruker(bruker: Bruker): Long {
        val eksisterendeBruker =
            BrukerTable.selectAll().where {
                (BrukerTable.brukerId eq bruker.id) and (BrukerTable.type eq bruker.type)
            }.singleOrNull()
        return if (eksisterendeBruker != null) {
            eksisterendeBruker[BrukerTable.id].value
        } else {
            BrukerTable.insertAndGetId {
                it[brukerId] = bruker.id
                it[type] = bruker.type
            }.value
        }
    }

    private fun settInnArbeidssoekerperiode(
        periodeId: UUID,
        identitetsnummer: String,
        startetId: Long,
        avsluttetId: Long?
    ) {
        PeriodeTable.insert {
            it[PeriodeTable.periodeId] = periodeId
            it[PeriodeTable.identitetsnummer] = identitetsnummer
            it[PeriodeTable.startetId] = startetId
            it[PeriodeTable.avsluttetId] = avsluttetId
        }
    }

    fun oppdaterArbeidssoekerperiode(arbeidssoekerperiode: Periode) {
        transaction(database) {
            try {
                if (arbeidssoekerperiode.avsluttet != null) {
                    oppdaterAvsluttetMetadata(arbeidssoekerperiode.id, arbeidssoekerperiode.avsluttet)
                } else {
                    throw IllegalArgumentException("Avsluttet kan ikke være null ved oppdatering av periode")
                }
            } catch (e: SQLException) {
                logger.error("Feil ved oppdatering av periode", e)
            }
        }
    }

    private fun oppdaterAvsluttetMetadata(
        periodeId: UUID,
        avsluttetMetadata: Metadata
    ) {
        val avsluttetMetadataId = settInnMetadata(avsluttetMetadata)
        PeriodeTable.update({ PeriodeTable.periodeId eq periodeId }) {
            it[avsluttetId] = avsluttetMetadataId
        }
    }
}

class PeriodeConverter(private val repository: ArbeidssoekerperiodeRepository) {
    fun konverterResultRowToPeriode(resultRow: ResultRow): Periode {
        val periodeId = resultRow[PeriodeTable.periodeId]
        val identitetsnummer = resultRow[PeriodeTable.identitetsnummer]
        val startetId = resultRow[PeriodeTable.startetId]
        val avsluttetId = resultRow[PeriodeTable.avsluttetId]

        val startetMetadata = repository.hentMetadata(startetId) ?: throw Error("Fant ikke startet metadata")
        val avsluttetMetadata = avsluttetId?.let { repository.hentMetadata(it) }

        return Periode(
            periodeId,
            identitetsnummer,
            startetMetadata,
            avsluttetMetadata
        )
    }
}
