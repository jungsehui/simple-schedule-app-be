package com.geekchat.server.domain.model

import java.time.Instant

data class Message(
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val clientMessageId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val expiresAt: Instant? = null,
    val replyToMessageId: String? = null,
    val burnAfterRead: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
)
