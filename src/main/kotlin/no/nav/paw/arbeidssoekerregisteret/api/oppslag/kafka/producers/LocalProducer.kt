package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.producers

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.OpplysningerOmArbeidssoekerSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.PeriodeSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.ProfileringSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TopicUtils
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE)

    produserMeldinger(kafkaConfig, applicationConfig, TopicUtils()::lagTestPerioder, applicationConfig.periodeTopic, PeriodeSerializer())
    produserMeldinger(kafkaConfig, applicationConfig, TopicUtils()::lagTestOpplysningerOmArbeidssoeker, applicationConfig.opplysningerOmArbeidssoekerTopic, OpplysningerOmArbeidssoekerSerializer())
    produserMeldinger(kafkaConfig, applicationConfig, TopicUtils()::lagTestProfilering, applicationConfig.profileringTopic, ProfileringSerializer())
}

fun <T : SpecificRecord> produserMeldinger(
    kafkaConfig: KafkaConfig,
    applicationConfig: ApplicationConfig,
    lagTestFunction: () -> List<T>,
    topic: String,
    serializer: Serializer<T>
) {
    val localProducer = LocalProducer(kafkaConfig, applicationConfig, serializer)
    try {
        lagTestFunction().forEach { message ->
            localProducer.produceMessage(topic, 1234L, message)
        }
    } catch (e: Exception) {
        println("LocalProducer $topic error: ${e.message}")
    } finally {
        localProducer.closeProducer()
    }
}

class LocalProducer<T : SpecificRecord>(
    private val kafkaConfig: KafkaConfig,
    private val applicationConfig: ApplicationConfig,
    private val valueSerializer: Serializer<T>
) {
    private lateinit var producer: Producer<Long, T>

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer =
            kafkaFactory.createProducer<Long, T>(
                clientId = applicationConfig.gruppeId,
                keySerializer = LongSerializer::class,
                valueSerializer = valueSerializer::class
            )
    }

    fun produceMessage(
        topic: String,
        key: Long,
        value: T
    ) {
        val record = ProducerRecord(topic, key, value)
        producer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send message to topic $topic: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }
    }

    fun closeProducer() {
        producer.close()
    }
}
