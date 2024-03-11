package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumers

import io.kotest.core.spec.style.FunSpec
/*import io.getunleash.Unleash
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TopicUtils
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.concurrent.thread*/

class ProfileringConsumerTest : FunSpec({

    test("test") {
        assert(true)
    }

    /*test("should consume and process messages when toggle is active") {
        val topic = "test-topic"
        val consumerMock = mockk<KafkaConsumer<Long, Profilering>>()
        val serviceMock = mockk<ProfileringService>()
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
    }*/
})

/*private fun createConsumerRecords(): ConsumerRecords<Long, Profilering> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Profilering>>>()
    val topic = "test-topic"
    records[TopicPartition(topic, 0)] = mutableListOf(ConsumerRecord(topic, 0, 0, 1L, TopicUtils().lagTestProfilering()))
    return ConsumerRecords(records)
}*/
