package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.application.port.out.MessageRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.domain.error.ChatError
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import com.geekchat.server.domain.model.ChatRoomType
import com.geekchat.server.domain.model.Message
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class MessageServiceTest {

    private val messageRepository = mockk<MessageRepository>()
    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val chatRoomMemberRepository = mockk<ChatRoomMemberRepository>()
    private val userRepository = mockk<UserRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher> {
        every { publishEvent(any<Any>()) } returns Unit
    }

    private lateinit var service: MessageService

    @BeforeEach
    fun setUp() {
        service = MessageService(messageRepository, chatRoomRepository, chatRoomMemberRepository, userRepository, eventPublisher)
    }

    @Test
    fun `sendMessage succeeds and publishes event`() {
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true
        val msgSlot = slot<Message>()
        every { messageRepository.save(capture(msgSlot)) } answers { msgSlot.captured }
        every { chatRoomRepository.findById("r1") } returns ChatRoom(id = "r1", type = ChatRoomType.DIRECT)
        val roomSlot = slot<ChatRoom>()
        every { chatRoomRepository.save(capture(roomSlot)) } answers { roomSlot.captured }

        val result = service.sendMessage("r1", "u1", "Hello", "cid1")

        assertTrue(result.isRight)
        assertEquals("Hello", result.getOrNull()!!.content)
        verify(exactly = 1) { eventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `sendMessage returns existing message for duplicate clientMessageId`() {
        val existing = Message(id = "m1", chatRoomId = "r1", senderId = "u1", clientMessageId = "cid1", content = "Hello")
        every { messageRepository.findByClientMessageId("cid1") } returns existing

        val result = service.sendMessage("r1", "u1", "Hello", "cid1")

        assertTrue(result.isRight)
        assertEquals("m1", result.getOrNull()!!.id)
        verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `sendMessage fails when not a member`() {
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns false

        val result = service.sendMessage("r1", "u1", "Hello", "cid1")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.NotRoomMember)
    }

    @Test
    fun `sendMessage fails with empty content`() {
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true

        val result = service.sendMessage("r1", "u1", "  ", "cid1")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.EmptyMessage)
    }

    @Test
    fun `markAsRead updates lastReadAt`() {
        val message = Message(id = "m1", chatRoomId = "r1", senderId = "u2", clientMessageId = "c1", content = "Hi")
        val member = ChatRoomMember(id = "mb1", userId = "u1", chatRoomId = "r1")

        every { messageRepository.findById("m1") } returns message
        every { chatRoomMemberRepository.findByUserIdAndChatRoomId("u1", "r1") } returns member
        val memberSlot = slot<ChatRoomMember>()
        every { chatRoomMemberRepository.save(capture(memberSlot)) } answers { memberSlot.captured }

        val result = service.markAsRead("r1", "u1", "m1")

        assertTrue(result.isRight)
        verify(exactly = 1) { chatRoomMemberRepository.save(any()) }
    }

    @Test
    fun `markAsRead fails when message not found`() {
        every { messageRepository.findById("missing") } returns null

        val result = service.markAsRead("r1", "u1", "missing")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.MessageNotFound)
    }
}
