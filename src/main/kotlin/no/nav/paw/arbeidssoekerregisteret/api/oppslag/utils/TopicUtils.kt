package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import java.time.Instant
import java.util.UUID

class TopicUtils {
    val testPeriodeId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val testPeriodeId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val testOpplysningerId1 = UUID.fromString("00000000-0000-0000-0000-000000000003")

    fun lagTestPerioder(): List<Periode> {
        return listOf(
            Periode(
                testPeriodeId1,
                "12345678911",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test"
                ),
                null
            ),
            Periode(
                testPeriodeId2,
                "12345678911",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test"
                ),
                Metadata(
                    Instant.now().plusSeconds(100),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test"
                )
            )
        )
    }

    fun lagTestOpplysningerOmArbeidssoeker(): List<OpplysningerOmArbeidssoeker> {
        return listOf(
            OpplysningerOmArbeidssoeker(
                testOpplysningerId1,
                testPeriodeId1,
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678911"
                    ),
                    "test",
                    "test"
                ),
                Utdanning(
                    "NUS_KODE",
                    JaNeiVetIkke.JA,
                    JaNeiVetIkke.JA
                ),
                Helse(
                    JaNeiVetIkke.JA
                ),
                Jobbsituasjon(
                    listOf(
                        BeskrivelseMedDetaljer(
                            Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                            mapOf(
                                Pair("test", "test"),
                                Pair("test2", "test2")
                            )
                        ),
                        BeskrivelseMedDetaljer(
                            Beskrivelse.DELTIDSJOBB_VIL_MER,
                            mapOf(
                                Pair("test3", "test3"),
                                Pair("test4", "test4")
                            )
                        )
                    )
                ),
                Annet(
                    JaNeiVetIkke.JA
                )
            )
        )
    }

    fun lagTestProfilering(): Profilering {
        return Profilering(
            UUID.randomUUID(),
            testPeriodeId1,
            testOpplysningerId1,
            Metadata(
                Instant.now(),
                Bruker(
                    BrukerType.UKJENT_VERDI,
                    "12345678911"
                ),
                "test",
                "test"
            ),
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            true,
            30
        )
    }
}
