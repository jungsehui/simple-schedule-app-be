package com.geekchat.server.infrastructure.scheduler

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.application.port.out.MessageRepository
import com.geekchat.server.domain.event.ChatEvent
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.time.temporal.ChronoUnit

class RoomExpirationSchedulerTest {

    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val chatRoomMemberRepository = mockk<ChatRoomMemberRepository>()
    private val messageRepository = mockk<MessageRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher> { every { publishEvent(any<Any>()) } returns Unit }

    private lateinit var scheduler: RoomExpirationScheduler

    @BeforeEach
    fun setUp() {
        scheduler = RoomExpirationScheduler(chatRoomRepository, chatRoomMemberRepository, messageRepository, eventPublisher)
    }

    @Test
    fun `cleanup soft-deletes expired rooms`() {
        val expiredRoom = ChatRoom(id = "r1", type = ChatRoomType.GROUP, name = "Expired", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        every { chatRoomRepository.findExpiringRoomsSoon(any(), any()) } returns emptyList()
        every { chatRoomRepository.findExpiredRooms(any()) } returns listOf(expiredRoom)
        every { messageRepository.softDeleteByRoomId(any(), any()) } just runs
        every { chatRoomMemberRepository.deleteAllByChatRoomId(any()) } just runs
        every { chatRoomRepository.softDelete(any(), any()) } just runs

        scheduler.processExpiringRooms()

        verify { messageRepository.softDeleteByRoomId("r1", any()) }
        verify { chatRoomMemberRepository.deleteAllByChatRoomId("r1") }
        verify { chatRoomRepository.softDelete("r1", any()) }
    }

    @Test
    fun `cleanup publishes RoomExpired event`() {
        val expiredRoom = ChatRoom(id = "r1", type = ChatRoomType.GROUP, name = "Expired", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        every { chatRoomRepository.findExpiringRoomsSoon(any(), any()) } returns emptyList()
        every { chatRoomRepository.findExpiredRooms(any()) } returns listOf(expiredRoom)
        every { messageRepository.softDeleteByRoomId(any(), any()) } just runs
        every { chatRoomMemberRepository.deleteAllByChatRoomId(any()) } just runs
        every { chatRoomRepository.softDelete(any(), any()) } just runs

        scheduler.processExpiringRooms()

        val eventSlot = slot<Any>()
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
    }

    @Test
    fun `warning publishes RoomExpiring for expiring rooms`() {
        val expiringRoom = ChatRoom(id = "r2", type = ChatRoomType.GROUP, name = "Soon", expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES))
        every { chatRoomRepository.findExpiringRoomsSoon(any(), any()) } returns listOf(expiringRoom)
        every { chatRoomRepository.findExpiredRooms(any()) } returns emptyList()

        scheduler.processExpiringRooms()

        verify { eventPublisher.publishEvent(any<ChatEvent.RoomExpiring>()) }
    }

    @Test
    fun `does nothing when no rooms expired or expiring`() {
        every { chatRoomRepository.findExpiringRoomsSoon(any(), any()) } returns emptyList()
        every { chatRoomRepository.findExpiredRooms(any()) } returns emptyList()

        scheduler.processExpiringRooms()

        verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
    }
}
