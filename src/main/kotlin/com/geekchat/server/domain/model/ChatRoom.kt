package com.geekchat.server.domain.model

import java.time.Duration
import java.time.Instant

data class ChatRoom(
    val id: String,
    val type: ChatRoomType,
    val name: String? = null,
    val lastMessageAt: Instant? = null,
    val expiresAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
) {
    companion object {
        const val MAX_DIRECT_MEMBERS = 2
        const val MAX_GROUP_MEMBERS = 100
    }

    val maxMembers: Int
        get() = when (type) {
            ChatRoomType.DIRECT -> MAX_DIRECT_MEMBERS
            ChatRoomType.GROUP -> MAX_GROUP_MEMBERS
        }

    fun withLastMessageAt(at: Instant = Instant.now()): ChatRoom =
        copy(lastMessageAt = at, updatedAt = Instant.now())

    fun isExpired(now: Instant = Instant.now()): Boolean =
        expiresAt != null && now >= expiresAt

    fun isExpiringSoon(now: Instant = Instant.now(), threshold: Duration = Duration.ofMinutes(10)): Boolean =
        expiresAt != null && !isExpired(now) && now >= expiresAt.minus(threshold)

    fun validateInvariant() {
        if (type == ChatRoomType.DIRECT) {
            require(name == null) { "Direct rooms must not have a name" }
        }
    }
}
