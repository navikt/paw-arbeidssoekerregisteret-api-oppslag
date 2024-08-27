package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnOpplysninger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.lagreOpplysninger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.lagrePeriodeOpplysninger
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerOmArbeidssoekerRepository(private val database: Database) {

    fun hentOpplysningerOmArbeidssoeker(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> =
        transaction(database) {
            finnOpplysninger(periodeId)
        }

    fun hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(identitetsnummer: Identitetsnummer): List<OpplysningerOmArbeidssoekerResponse> =
        transaction(database) {
            // TODO Optimalisering vha joins
            val periodeIder = ArbeidssoekerperiodeRepository(database).hentArbeidssoekerperioder(identitetsnummer).map { it.periodeId }
            periodeIder.flatMap { periodeId ->
                finnOpplysninger(periodeId)
            }
        }

    fun lagreOpplysningerOmArbeidssoeker(opplysninger: OpplysningerOmArbeidssoeker) {
        transaction(database) {
            val eksisterendeOpplysninger = finnOpplysningerRow(opplysninger.id)

            if (eksisterendeOpplysninger != null) {
                if (eksisterendeOpplysninger.periodeId == null || eksisterendeOpplysninger.periodeId != opplysninger.periodeId) {
                    lagrePeriodeOpplysninger(eksisterendeOpplysninger.id, opplysninger.periodeId)
                } else {
                }
            } else {
                lagreOpplysninger(opplysninger)
            }
        }
    }

    fun storeBatch(batch: Sequence<OpplysningerOmArbeidssoeker>) {
        transaction(database) {
            maxAttempts = 2
            minRetryDelay = 20
            batch.forEach { opplysninger ->
                lagreOpplysningerOmArbeidssoeker(opplysninger)
            }
        }
    }
}

private data class OpplysningerRow(
    val id: Long,
    val opplysningerId: UUID,
    val periodeId: UUID?
)

private fun Transaction.finnOpplysningerRow(opplysningerId: UUID): OpplysningerRow? {
    return OpplysningerOmArbeidssoekerTable
        .join(PeriodeOpplysningerTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.id, PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId)
        .selectAll()
        .where { OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId eq opplysningerId }
        .singleOrNull()?.toOpplysningerRow()
}

private fun ResultRow.toOpplysningerRow(): OpplysningerRow {
    val id = get(OpplysningerOmArbeidssoekerTable.id).value
    val opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId)
    val periodeId = get(PeriodeOpplysningerTable.periodeId)
    return OpplysningerRow(id, opplysningerId, periodeId)
}
