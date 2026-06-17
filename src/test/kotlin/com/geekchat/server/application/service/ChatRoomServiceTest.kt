package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.user.domain.repository.UserRepository
import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import com.geekchat.server.domain.model.ChatRoomType
import com.geekchat.server.user.domain.model.User
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatRoomServiceTest {

    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val chatRoomMemberRepository = mockk<ChatRoomMemberRepository>()
    private val userRepository = mockk<UserRepository>()
    private val webSocketBroadcaster = mockk<WebSocketBroadcaster>()

    private lateinit var service: ChatRoomService

    @BeforeEach
    fun setUp() {
        every { webSocketBroadcaster.joinRoom(any(), any()) } just runs
        service = ChatRoomService(chatRoomRepository, chatRoomMemberRepository, userRepository, webSocketBroadcaster)
    }

    @Test
    fun `createRoom DIRECT returns existing room`() {
        val existingRoom = ChatRoom(id = "r1", type = ChatRoomType.DIRECT)
        val member1 = ChatRoomMember(id = "m1", userId = "u1", chatRoomId = "r1")
        val member2 = ChatRoomMember(id = "m2", userId = "u2", chatRoomId = "r1")

        every { userRepository.findById("u2") } returns User(id = "u2", nickname = "Bob")
        every { chatRoomRepository.findDirectRoomBetween("u1", "u2") } returns existingRoom
        every { chatRoomMemberRepository.findAllByChatRoomId("r1") } returns listOf(member1, member2)
        every { userRepository.findById("u1") } returns User(id = "u1", nickname = "Alice")

        val result = service.createRoom("u1", listOf("u2"), null)

        assertTrue(result.isRight)
        assertEquals("r1", result.getOrNull()!!.room.id)
        assertEquals(ChatRoomType.DIRECT, result.getOrNull()!!.room.type)
    }

    @Test
    fun `createRoom DIRECT creates new room when none exists`() {
        every { userRepository.findById("u2") } returns User(id = "u2", nickname = "Bob")
        every { chatRoomRepository.findDirectRoomBetween("u1", "u2") } returns null
        val roomSlot = slot<ChatRoom>()
        every { chatRoomRepository.save(capture(roomSlot)) } answers { roomSlot.captured }
        val memberSlot = slot<ChatRoomMember>()
        every { chatRoomMemberRepository.save(capture(memberSlot)) } answers { memberSlot.captured }
        every { chatRoomMemberRepository.findAllByChatRoomId(any()) } returns listOf(
            ChatRoomMember(id = "m1", userId = "u1", chatRoomId = "new"),
            ChatRoomMember(id = "m2", userId = "u2", chatRoomId = "new"),
        )
        every { userRepository.findById("u1") } returns User(id = "u1", nickname = "Alice")

        val result = service.createRoom("u1", listOf("u2"), null)

        assertTrue(result.isRight)
        assertEquals(ChatRoomType.DIRECT, result.getOrNull()!!.room.type)
        assertEquals(2, result.getOrNull()!!.members.size)
    }

    @Test
    fun `createRoom GROUP creates room with name`() {
        every { userRepository.findById("u2") } returns User(id = "u2", nickname = "Bob")
        every { userRepository.findById("u3") } returns User(id = "u3", nickname = "Carol")
        val roomSlot = slot<ChatRoom>()
        every { chatRoomRepository.save(capture(roomSlot)) } answers { roomSlot.captured }
        val memberSlot = slot<ChatRoomMember>()
        every { chatRoomMemberRepository.save(capture(memberSlot)) } answers { memberSlot.captured }
        every { chatRoomMemberRepository.findAllByChatRoomId(any()) } returns listOf(
            ChatRoomMember(id = "m1", userId = "u1", chatRoomId = "new"),
            ChatRoomMember(id = "m2", userId = "u2", chatRoomId = "new"),
            ChatRoomMember(id = "m3", userId = "u3", chatRoomId = "new"),
        )
        every { userRepository.findById("u1") } returns User(id = "u1", nickname = "Alice")

        val result = service.createRoom("u1", listOf("u2", "u3"), "Team")

        assertTrue(result.isRight)
        assertEquals(ChatRoomType.GROUP, result.getOrNull()!!.room.type)
        assertEquals("Team", result.getOrNull()!!.room.name)
    }

    @Test
    fun `createRoom fails when other user not found`() {
        every { userRepository.findById("missing") } returns null

        val result = service.createRoom("u1", listOf("missing"), null)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.UserNotFound)
    }

    // ────── Mute Room (M2 P0) ──────

    @Test
    fun `setRoomMuted succeeds for member`() {
        val member = ChatRoomMember(id = "m1", userId = "u1", chatRoomId = "r1", muted = false)
        every { chatRoomMemberRepository.findByUserIdAndChatRoomId("u1", "r1") } returns member
        val saved = slot<ChatRoomMember>()
        every { chatRoomMemberRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.setRoomMuted("u1", "r1", true)

        assertTrue(result.isRight)
        assertTrue(saved.captured.muted)
    }

    @Test
    fun `setRoomMuted fails when not member`() {
        every { chatRoomMemberRepository.findByUserIdAndChatRoomId("u1", "r1") } returns null

        val result = service.setRoomMuted("u1", "r1", true)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.NotRoomMember)
    }
}
