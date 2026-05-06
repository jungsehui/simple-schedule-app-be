package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.application.port.out.InviteLinkRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.domain.error.ChatError
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import com.geekchat.server.domain.model.ChatRoomType
import com.geekchat.server.domain.model.InviteLink
import com.geekchat.server.domain.model.User
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class InviteLinkServiceTest {

    private val inviteLinkRepository = mockk<InviteLinkRepository>()
    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val chatRoomMemberRepository = mockk<ChatRoomMemberRepository>()
    private val userRepository = mockk<UserRepository>()
    private val webSocketBroadcaster = mockk<WebSocketBroadcaster> { every { joinRoom(any(), any()) } just runs }

    private lateinit var service: InviteLinkService

    @BeforeEach
    fun setUp() {
        service = InviteLinkService(inviteLinkRepository, chatRoomRepository, chatRoomMemberRepository, userRepository, webSocketBroadcaster)
    }

    @Test
    fun `createInviteLink succeeds for room member`() {
        every { chatRoomRepository.findById("r1") } returns ChatRoom(id = "r1", type = ChatRoomType.GROUP, name = "G")
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns true
        val linkSlot = slot<InviteLink>()
        every { inviteLinkRepository.save(capture(linkSlot)) } answers { linkSlot.captured }

        val result = service.createInviteLink("r1", "u1")
        assertTrue(result.isRight)
        assertEquals(8, result.getOrNull()!!.code.length)
    }

    @Test
    fun `createInviteLink fails when room not found`() {
        every { chatRoomRepository.findById("missing") } returns null

        val result = service.createInviteLink("missing", "u1")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.RoomNotFound)
    }

    @Test
    fun `createInviteLink fails when not room member`() {
        every { chatRoomRepository.findById("r1") } returns ChatRoom(id = "r1", type = ChatRoomType.GROUP, name = "G")
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u1", "r1") } returns false

        val result = service.createInviteLink("r1", "u1")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.NotRoomMember)
    }

    @Test
    fun `joinByInviteCode succeeds`() {
        val link = InviteLink(id = "l1", code = "ABC12345", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS))
        val room = ChatRoom(id = "r1", type = ChatRoomType.GROUP, name = "G")

        every { inviteLinkRepository.findByCode("ABC12345") } returns link
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u2", "r1") } returns false
        every { chatRoomRepository.findById("r1") } returns room
        every { chatRoomRepository.countMembers("r1") } returns 2
        val memberSlot = slot<ChatRoomMember>()
        every { chatRoomMemberRepository.save(capture(memberSlot)) } answers { memberSlot.captured }
        val linkSlot = slot<InviteLink>()
        every { inviteLinkRepository.save(capture(linkSlot)) } answers { linkSlot.captured }
        every { chatRoomMemberRepository.findAllByChatRoomId("r1") } returns listOf(
            ChatRoomMember(id = "m1", userId = "u1", chatRoomId = "r1"),
            ChatRoomMember(id = "m2", userId = "u2", chatRoomId = "r1"),
        )
        every { userRepository.findById("u1") } returns User(id = "u1", nickname = "Alice")
        every { userRepository.findById("u2") } returns User(id = "u2", nickname = "Bob")

        val result = service.joinByInviteCode("ABC12345", "u2")
        assertTrue(result.isRight)
        assertEquals(2, result.getOrNull()!!.members.size)
        verify { webSocketBroadcaster.joinRoom("u2", "r1") }
    }

    @Test
    fun `joinByInviteCode fails when link expired`() {
        val link = InviteLink(id = "l1", code = "ABC12345", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        every { inviteLinkRepository.findByCode("ABC12345") } returns link

        val result = service.joinByInviteCode("ABC12345", "u2")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InviteLinkExpired)
    }

    @Test
    fun `joinByInviteCode fails when max uses reached`() {
        val link = InviteLink(id = "l1", code = "ABC12345", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS), maxUses = 3, currentUses = 3)
        every { inviteLinkRepository.findByCode("ABC12345") } returns link

        val result = service.joinByInviteCode("ABC12345", "u2")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InviteLinkMaxUsesReached)
    }

    @Test
    fun `joinByInviteCode fails when already member`() {
        val link = InviteLink(id = "l1", code = "ABC12345", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS))
        every { inviteLinkRepository.findByCode("ABC12345") } returns link
        every { chatRoomMemberRepository.existsByUserIdAndChatRoomId("u2", "r1") } returns true

        val result = service.joinByInviteCode("ABC12345", "u2")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.AlreadyRoomMember)
    }

    @Test
    fun `joinByInviteCode fails when code not found`() {
        every { inviteLinkRepository.findByCode("INVALID") } returns null

        val result = service.joinByInviteCode("INVALID", "u2")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InviteLinkNotFound)
    }
}
