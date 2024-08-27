package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toMetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.endrePeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnPerioder
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.lagrePeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ArbeidssoekerperiodeRepository(private val database: Database) {
    fun hentArbeidssoekerperiode(periodeId: UUID): Periode? =
        transaction(database) {
            finnPeriode(periodeId)
        }

    fun hentArbeidssoekerperioder(identitetsnummer: Identitetsnummer): List<ArbeidssoekerperiodeResponse> =
        transaction(database) {
            finnPerioder(identitetsnummer)
                .map { ArbeidssoekerperiodeResponse(it.id, it.startet.toMetadataResponse(), it.avsluttet?.toMetadataResponse()) }
        }

    fun hentAntallAktivePerioder(): Long =
        transaction(database) {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun opprettArbeidssoekerperiode(periode: Periode) {
        transaction(database) {
            lagrePeriode(periode)
        }
    }

    fun oppdaterArbeidssoekerperiode(periode: Periode) {
        transaction(database) {
            endrePeriode(periode)
        }
    }

    fun storeBatch(perioder: Sequence<Periode>) {
        transaction(database) {
            maxAttempts = 2
            minRetryDelay = 20

            val periodeIdList = perioder.map { it.id }.toList()
            val eksisterendePerioder = finnPeriodeRows(periodeIdList)
            val eksisterendePeriodeIdList = eksisterendePerioder.map { it.periodeId }.toHashSet()

            perioder.forEach { periode ->
                if (eksisterendePeriodeIdList.contains(periode.id)) {
                    endrePeriode(periode)
                } else {
                    lagrePeriode(periode)
                }
            }
        }
    }
}

private data class PeriodeRow(
    val periodeId: UUID,
)

private fun Transaction.finnPeriodeRows(periodeIdList: List<UUID>): List<PeriodeRow> {
    return PeriodeTable.selectAll().where { PeriodeTable.periodeId inList periodeIdList }.map { it.toPeriodeRow() }
}

private fun ResultRow.toPeriodeRow(): PeriodeRow {
    val periodeId = get(PeriodeTable.periodeId)
    return PeriodeRow(periodeId)
}
