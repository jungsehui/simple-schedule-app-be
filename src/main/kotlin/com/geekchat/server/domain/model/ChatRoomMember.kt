package com.geekchat.server.domain.model

import java.time.Instant

data class ChatRoomMember(
    val id: String,
    val userId: String,
    val chatRoomId: String,
    val joinedAt: Instant = Instant.now(),
    val lastReadAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun withMarkReadAt(readAt: Instant): ChatRoomMember {
        if (lastReadAt != null && lastReadAt >= readAt) {
            return this
        }
        return copy(lastReadAt = readAt, updatedAt = Instant.now())
    }
}
