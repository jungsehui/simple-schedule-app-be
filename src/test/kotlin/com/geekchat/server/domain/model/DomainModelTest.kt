package com.geekchat.server.domain.model
import com.geekchat.server.user.domain.model.User

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class DomainModelTest {

    // ── User ──

    @Test
    fun `User withUsername accepts valid username`() {
        val user = User(id = "1", nickname = "test")
        val updated = user.withUsername("john_doe")
        assertEquals("john_doe", updated.username)
    }

    @Test
    fun `User withUsername rejects invalid username`() {
        val user = User(id = "1", nickname = "test")
        assertThrows<IllegalArgumentException> { user.withUsername("AB") }
        assertThrows<IllegalArgumentException> { user.withUsername("has space") }
        assertThrows<IllegalArgumentException> { user.withUsername("UPPER") }
        assertThrows<IllegalArgumentException> { user.withUsername("a".repeat(21)) }
    }

    @Test
    fun `User withUpdatedProfile rejects blank nickname`() {
        val user = User(id = "1", nickname = "test")
        assertThrows<IllegalArgumentException> { user.withUpdatedProfile("") }
        assertThrows<IllegalArgumentException> { user.withUpdatedProfile("a".repeat(21)) }
    }

    // ── ChatRoom ──

    @Test
    fun `ChatRoom DIRECT maxMembers is 2`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.DIRECT)
        assertEquals(2, room.maxMembers)
    }

    @Test
    fun `ChatRoom GROUP maxMembers is 100`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "Team")
        assertEquals(100, room.maxMembers)
    }

    @Test
    fun `ChatRoom validateInvariant fails for DIRECT with name`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.DIRECT, name = "Bad")
        assertThrows<IllegalArgumentException> { room.validateInvariant() }
    }

    @Test
    fun `ChatRoom validateInvariant passes for DIRECT without name`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.DIRECT)
        room.validateInvariant() // should not throw
    }

    @Test
    fun `ChatRoom withLastMessageAt updates timestamp`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.DIRECT)
        assertNull(room.lastMessageAt)
        val updated = room.withLastMessageAt()
        assertTrue(updated.lastMessageAt != null)
    }

    // ── ChatRoomMember ──

    @Test
    fun `ChatRoomMember withMarkReadAt advances forward`() {
        val t1 = Instant.now()
        val t2 = t1.plus(1, ChronoUnit.HOURS)
        val member = ChatRoomMember(id = "1", userId = "u1", chatRoomId = "r1", lastReadAt = t1)
        val updated = member.withMarkReadAt(t2)
        assertEquals(t2, updated.lastReadAt)
    }

    @Test
    fun `ChatRoomMember withMarkReadAt does not go backward`() {
        val t1 = Instant.now()
        val t0 = t1.minus(1, ChronoUnit.HOURS)
        val member = ChatRoomMember(id = "1", userId = "u1", chatRoomId = "r1", lastReadAt = t1)
        val same = member.withMarkReadAt(t0)
        assertTrue(same === member) // exact same instance
    }

    @Test
    fun `ChatRoomMember withMarkReadAt from null`() {
        val t1 = Instant.now()
        val member = ChatRoomMember(id = "1", userId = "u1", chatRoomId = "r1")
        assertNull(member.lastReadAt)
        val updated = member.withMarkReadAt(t1)
        assertEquals(t1, updated.lastReadAt)
    }

    // ── RefreshToken ──

    @Test
    fun `RefreshToken isExpired returns true for past date`() {
        val token = RefreshToken(
            id = "1", userId = "u1", token = "t",
            expiresAt = Instant.now().minus(1, ChronoUnit.DAYS),
        )
        assertTrue(token.isExpired())
    }

    @Test
    fun `RefreshToken isExpired returns false for future date`() {
        val token = RefreshToken(
            id = "1", userId = "u1", token = "t",
            expiresAt = Instant.now().plus(14, ChronoUnit.DAYS),
        )
        assertFalse(token.isExpired())
    }

    // ── ChatRoom expiration ──

    @Test
    fun `ChatRoom isExpired returns true when expiresAt in past`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "G", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        assertTrue(room.isExpired())
    }

    @Test
    fun `ChatRoom isExpired returns false when expiresAt null`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "G")
        assertFalse(room.isExpired())
    }

    @Test
    fun `ChatRoom isExpired returns false when expiresAt in future`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "G", expiresAt = Instant.now().plus(1, ChronoUnit.HOURS))
        assertFalse(room.isExpired())
    }

    @Test
    fun `ChatRoom isExpiringSoon returns true within threshold`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "G", expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES))
        assertTrue(room.isExpiringSoon(threshold = Duration.ofMinutes(10)))
    }

    @Test
    fun `ChatRoom isExpiringSoon returns false when far from expiry`() {
        val room = ChatRoom(id = "1", type = ChatRoomType.GROUP, name = "G", expiresAt = Instant.now().plus(1, ChronoUnit.HOURS))
        assertFalse(room.isExpiringSoon(threshold = Duration.ofMinutes(10)))
    }

    // ── InviteLink ──

    @Test
    fun `InviteLink isExpired returns true for past date`() {
        val link = InviteLink(id = "1", code = "ABCD1234", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        assertTrue(link.isExpired())
    }

    @Test
    fun `InviteLink isMaxUsesReached returns true when currentUses equals maxUses`() {
        val link = InviteLink(id = "1", code = "ABCD1234", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS), maxUses = 5, currentUses = 5)
        assertTrue(link.isMaxUsesReached())
    }

    @Test
    fun `InviteLink isMaxUsesReached returns false when maxUses is null`() {
        val link = InviteLink(id = "1", code = "ABCD1234", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS), currentUses = 100)
        assertFalse(link.isMaxUsesReached())
    }

    @Test
    fun `InviteLink withIncrementedUses increments`() {
        val link = InviteLink(id = "1", code = "ABCD1234", roomId = "r1", creatorId = "u1", expiresAt = Instant.now().plus(1, ChronoUnit.DAYS), currentUses = 3)
        assertEquals(4, link.withIncrementedUses().currentUses)
    }

    @Test
    fun `InviteLink generateCode returns 8 char string`() {
        val code = InviteLink.generateCode()
        assertEquals(8, code.length)
        assertTrue(code.all { it.isLetterOrDigit() })
    }
}
