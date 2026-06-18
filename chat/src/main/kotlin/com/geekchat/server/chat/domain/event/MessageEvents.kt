package com.geekchat.server.chat.domain.event

import com.geekchat.server.chat.domain.model.MessageType
import java.time.Instant

data class MessageSent(
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val content: String,
    val messageType: MessageType,
    val createdAt: Instant,
    val clientMessageId: String,
    val expiresAt: Instant? = null,
    val replyToMessageId: String? = null,
    val burnAfterRead: Boolean = false,
)

data class MessageRead(
    val roomId: String,
    val userId: String,
    val lastReadAt: Instant,
)

data class MessageExpired(
    val roomId: String,
    val messageIds: List<String>,
)

/** Burn-on-Read: receiver opened a burnAfterRead message → hard-deleted, broadcast to room. */
data class MessageBurned(
    val roomId: String,
    val messageId: String,
)
