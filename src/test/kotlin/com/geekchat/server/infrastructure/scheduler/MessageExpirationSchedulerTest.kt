package com.geekchat.server.infrastructure.scheduler

import com.geekchat.server.chat.domain.repository.MessageRepository
import com.geekchat.server.chat.infrastructure.scheduler.MessageExpirationScheduler
import com.geekchat.server.domain.event.ChatEvent
import com.geekchat.server.chat.domain.model.Message
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.time.temporal.ChronoUnit

class MessageExpirationSchedulerTest {

    private val messageRepository = mockk<MessageRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher> { every { publishEvent(any<Any>()) } returns Unit }

    private lateinit var scheduler: MessageExpirationScheduler

    @BeforeEach
    fun setUp() {
        scheduler = MessageExpirationScheduler(messageRepository, eventPublisher)
    }

    @Test
    fun `cleanup hard-deletes expired messages and publishes event`() {
        val msg1 = Message(id = "m1", chatRoomId = "r1", senderId = "u1", clientMessageId = "c1", content = "hi", expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES))
        val msg2 = Message(id = "m2", chatRoomId = "r1", senderId = "u1", clientMessageId = "c2", content = "bye", expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES))

        every { messageRepository.findExpiredMessages(any()) } returns listOf(msg1, msg2)
        every { messageRepository.hardDeleteByIds(any()) } just runs

        scheduler.cleanupExpiredMessages()

        verify { messageRepository.hardDeleteByIds(listOf("m1", "m2")) }
        verify { eventPublisher.publishEvent(any<ChatEvent.MessageExpired>()) }
    }

    @Test
    fun `groups events by roomId`() {
        val msg1 = Message(id = "m1", chatRoomId = "r1", senderId = "u1", clientMessageId = "c1", content = "hi", expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES))
        val msg2 = Message(id = "m2", chatRoomId = "r2", senderId = "u1", clientMessageId = "c2", content = "bye", expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES))

        every { messageRepository.findExpiredMessages(any()) } returns listOf(msg1, msg2)
        every { messageRepository.hardDeleteByIds(any()) } just runs

        scheduler.cleanupExpiredMessages()

        verify(exactly = 2) { messageRepository.hardDeleteByIds(any()) }
        verify(exactly = 2) { eventPublisher.publishEvent(any<ChatEvent.MessageExpired>()) }
    }

    @Test
    fun `does nothing when no messages expired`() {
        every { messageRepository.findExpiredMessages(any()) } returns emptyList()

        scheduler.cleanupExpiredMessages()

        verify(exactly = 0) { messageRepository.hardDeleteByIds(any()) }
    }
}
