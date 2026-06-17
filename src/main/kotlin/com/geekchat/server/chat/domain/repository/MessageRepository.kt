package com.geekchat.server.chat.domain.repository

import com.geekchat.server.chat.domain.model.Message
import java.time.Instant

interface MessageRepository {
    fun findById(id: String): Message?
    fun findByClientMessageId(clientMessageId: String): Message?
    fun findByRoomId(
        roomId: String,
        cursor: Instant? = null,
        limit: Int = 50,
        direction: PaginationDirection = PaginationDirection.BACKWARD,
    ): List<Message>
    fun save(message: Message): Message
    fun findExpiredMessages(now: Instant): List<Message>
    fun hardDeleteByIds(ids: List<String>)
    fun softDeleteByRoomId(roomId: String, now: Instant)
}

enum class PaginationDirection {
    FORWARD,
    BACKWARD,
}
