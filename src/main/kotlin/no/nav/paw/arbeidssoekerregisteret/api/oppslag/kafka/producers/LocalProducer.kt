package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.producers

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.KafkaConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.properties
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.LocalProducerUtils
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.loadLocalConfiguration
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

fun main() {
    val config = loadLocalConfiguration()
    produserPeriodeMeldinger(config.kafka)
    produserOpplysningerOmArbeidssoekerMeldinger(config.kafka)
    produserProfileringMeldinger(config.kafka)
}

fun produserPeriodeMeldinger(kafkaConfig: KafkaConfig) {
    val localProducer = LocalProducer(kafkaConfig)
    try {
        LocalProducerUtils().lagTestPerioder().forEach { periode ->
            localProducer.producePeriodeMessage(kafkaConfig.periodeTopic, periode.id.toString(), periode)
        }
    } catch (e: Exception) {
        println("LocalProducer periode error: ${e.message}")
        localProducer.closePeriodeProducer()
    }
}

fun produserOpplysningerOmArbeidssoekerMeldinger(kafkaConfig: KafkaConfig) {
    val localProducer = LocalProducer(kafkaConfig)
    try {
        LocalProducerUtils().lagTestOpplysningerOmArbeidssoeker().forEach { opplysninger ->
            localProducer.produceOpplysningerOmArbeidssoekerMessage(
                kafkaConfig.opplysningerOmArbeidssoekerTopic,
                opplysninger.id.toString(),
                opplysninger
            )
        }
    } catch (e: Exception) {
        println("LocalProducer opplysninger-om-arbeidssoeker error: ${e.message}")
        localProducer.closeOpplysningerOmArbeidssoekerProducer()
    }
}

fun produserProfileringMeldinger(kafkaConfig: KafkaConfig) {
    val localProducer = LocalProducer(kafkaConfig)
    try {
        LocalProducerUtils().lagTestProfilering().let { profilering ->
            localProducer.produceProfileringMessage(kafkaConfig.profileringTopic, profilering.id.toString(), profilering)
        }
    } catch (e: Exception) {
        println("LocalProducer profilering error: ${e.message}")
        localProducer.closeProfileringProducer()
    }
}

class LocalProducer(private val kafkaConfig: KafkaConfig) {
    private val periodeProducer: Producer<String, Periode> = createProducer()
    private val opplysningerOmArbeidssoekerProducer: Producer<String, OpplysningerOmArbeidssoeker> = createProducer()
    private val profileringProducer: Producer<String, Profilering> = createProducer()

    private fun <T> createProducer(): Producer<String, T> {
        val props = kafkaConfig.properties.toMutableMap()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java.name
        props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = kafkaConfig.schemaRegistryConfig.url

        return KafkaProducer(props)
    }

    fun producePeriodeMessage(
        topic: String,
        key: String,
        value: Periode
    ) {
        val record = ProducerRecord(topic, key, value)
        periodeProducer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send periode message: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }.get()
    }

    fun produceOpplysningerOmArbeidssoekerMessage(
        topic: String,
        key: String,
        value: OpplysningerOmArbeidssoeker
    ) {
        val record = ProducerRecord(topic, key, value)
        opplysningerOmArbeidssoekerProducer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send opplysninger-om-arbeidssoeker message: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }.get()
    }

    fun produceProfileringMessage(
        topic: String,
        key: String,
        value: Profilering
    ) {
        val record = ProducerRecord(topic, key, value)
        profileringProducer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send profilering message: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }.get()
    }

    fun closePeriodeProducer() {
        periodeProducer.close()
    }

    fun closeOpplysningerOmArbeidssoekerProducer() {
        opplysningerOmArbeidssoekerProducer.close()
    }

    fun closeProfileringProducer() {
        profileringProducer.close()
    }
}
