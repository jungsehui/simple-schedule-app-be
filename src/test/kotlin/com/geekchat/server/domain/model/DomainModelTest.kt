package com.geekchat.server.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
}
