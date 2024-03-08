package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumers

import io.getunleash.Unleash
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TopicUtils
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.concurrent.thread

class ArbeidssoekerperiodeConsumerTest : FunSpec({

    test("should consume and process messages when toggle is active") {
        val topic = "test-topic"
        val consumerMock = mockk<KafkaConsumer<Long, Periode>>()
        val serviceMock = mockk<ArbeidssoekerperiodeService>()
        val unleashClientMock = mockk<Unleash>()

        val consumer = BatchConsumer(topic, consumerMock, serviceMock::lagreBatch, unleashClientMock)

        every { unleashClientMock.isEnabled("aktiver-kafka-konsumere") } returns true
        every { consumerMock.subscribe(listOf(topic)) } just Runs
        every { consumerMock.assignment() } returns setOf(TopicPartition(topic, 0))
        every { consumerMock.poll(any<Duration>()) } returns createConsumerRecords()
        every { serviceMock.lagreBatch(any()) } just Runs
        every { consumerMock.commitSync() } just Runs
        every { consumerMock.resume(any()) } just Runs

        thread {
            consumer.start()
        }

        verify { consumerMock.subscribe(listOf(topic)) }
        verify { consumerMock.assignment() }
        verify { consumerMock.poll(any<Duration>()) }
        verify { serviceMock.lagreBatch(any()) }
        verify { consumerMock.commitSync() }

        consumer.stop()
    }

    test("should not consume messages when toggle is inactive") {
        val topic = "test-topic"
        val consumerMock = mockk<KafkaConsumer<Long, Periode>>()
        val serviceMock = mockk<ArbeidssoekerperiodeService>()
        val unleashClientMock = mockk<Unleash>()

        val consumer = BatchConsumer(topic, consumerMock, serviceMock::lagreBatch, unleashClientMock)

        every { unleashClientMock.isEnabled("aktiver-kafka-konsumere") } returns false
        every { consumerMock.subscribe(listOf(topic)) } just Runs

        thread {
            consumer.start()
        }

        verify { consumerMock.subscribe(listOf(topic)) }
        verify(exactly = 0) { consumerMock.poll(any<Duration>()) }
        verify(exactly = 0) { serviceMock.lagreBatch(any()) }
        verify(exactly = 0) { consumerMock.commitSync() }

        consumer.stop()
    }
})

private fun createConsumerRecords(): ConsumerRecords<Long, Periode> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Periode>>>()
    val topic = "test-topic"
    records[TopicPartition(topic, 0)] = mutableListOf(ConsumerRecord(topic, 0, 0, 1L, TopicUtils().lagTestPerioder()[0]))
    return ConsumerRecords(records)
}
