package com.geekchat.server.room.domain.model

import java.time.Instant

data class ChatRoomMember(
    val id: String,
    val userId: String,
    val chatRoomId: String,
    val joinedAt: Instant = Instant.now(),
    val lastReadAt: Instant? = null,
    val muted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun withMarkReadAt(readAt: Instant): ChatRoomMember {
        if (lastReadAt != null && lastReadAt >= readAt) {
            return this
        }
        return copy(lastReadAt = readAt, updatedAt = Instant.now())
    }

    fun withMuted(value: Boolean): ChatRoomMember =
        if (muted == value) this else copy(muted = value, updatedAt = Instant.now())
}
