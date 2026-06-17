package com.geekchat.server.application.service

import com.geekchat.server.chat.application.service.MessageService
import com.geekchat.server.room.domain.repository.ChatRoomMemberRepository
import com.geekchat.server.room.domain.repository.ChatRoomRepository
import com.geekchat.server.chat.domain.repository.MessageRepository
import com.geekchat.server.user.domain.repository.UserRepository
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.room.domain.model.ChatRoom
import com.geekchat.server.room.domain.model.ChatRoomMember
import com.geekchat.server.room.domain.model.ChatRoomType
import com.geekchat.server.chat.domain.model.Message
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

    // ────── Reply (M2 P0) ──────

    @Test
    fun `sendMessage with replyToMessageId succeeds when target is in same room`() {
        val replyTo = Message(id = "m0", chatRoomId = "r1", senderId = "u2", clientMessageId = "c0", content = "Hi")
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { messageRepository.findById("m0") } returns replyTo
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true
        val msgSlot = slot<Message>()
        every { messageRepository.save(capture(msgSlot)) } answers { msgSlot.captured }
        every { chatRoomRepository.findById("r1") } returns ChatRoom(id = "r1", type = ChatRoomType.DIRECT)
        every { chatRoomRepository.save(any()) } answers { firstArg() }

        val result = service.sendMessage("r1", "u1", "Hello", "cid1", replyToMessageId = "m0")

        assertTrue(result.isRight)
        assertEquals("m0", msgSlot.captured.replyToMessageId)
    }

    @Test
    fun `sendMessage with replyToMessageId fails when target does not exist`() {
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true
        every { messageRepository.findById("m-missing") } returns null

        val result = service.sendMessage("r1", "u1", "Hello", "cid1", replyToMessageId = "m-missing")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.MessageNotFound)
    }

    @Test
    fun `sendMessage with replyToMessageId fails when target is in different room`() {
        val replyTo = Message(id = "m0", chatRoomId = "OTHER", senderId = "u2", clientMessageId = "c0", content = "Hi")
        every { messageRepository.findByClientMessageId("cid1") } returns null
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true
        every { messageRepository.findById("m0") } returns replyTo

        val result = service.sendMessage("r1", "u1", "Hello", "cid1", replyToMessageId = "m0")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.MessageNotFound)
    }

    // ────── Burn-on-Read (M2 spike) ──────

    @Test
    fun `markAsRead burns a burn-after-read message from another sender`() {
        val burnMsg = Message(
            id = "m1", chatRoomId = "r1", senderId = "u2", clientMessageId = "c1",
            content = "self-destruct", burnAfterRead = true,
        )
        val member = ChatRoomMember(id = "mb1", userId = "u1", chatRoomId = "r1")
        every { messageRepository.findById("m1") } returns burnMsg
        every { chatRoomMemberRepository.findByUserIdAndChatRoomId("u1", "r1") } returns member
        every { chatRoomMemberRepository.save(any()) } answers { firstArg() }
        io.mockk.justRun { messageRepository.hardDeleteByIds(listOf("m1")) }

        val result = service.markAsRead("r1", "u1", "m1")

        assertTrue(result.isRight)
        verify(exactly = 1) { messageRepository.hardDeleteByIds(listOf("m1")) }
    }

    @Test
    fun `markAsRead does NOT burn the sender's own burn-after-read message`() {
        val burnMsg = Message(
            id = "m1", chatRoomId = "r1", senderId = "u1", clientMessageId = "c1",
            content = "self-destruct", burnAfterRead = true,
        )
        val member = ChatRoomMember(id = "mb1", userId = "u1", chatRoomId = "r1")
        every { messageRepository.findById("m1") } returns burnMsg
        every { chatRoomMemberRepository.findByUserIdAndChatRoomId("u1", "r1") } returns member
        every { chatRoomMemberRepository.save(any()) } answers { firstArg() }

        val result = service.markAsRead("r1", "u1", "m1")

        assertTrue(result.isRight)
        verify(exactly = 0) { messageRepository.hardDeleteByIds(any()) }
    }
}
