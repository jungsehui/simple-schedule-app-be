package com.geekchat.server.domain.event

import com.geekchat.server.domain.model.MessageType
import java.time.Instant

sealed class ChatEvent {

    data class MessageSent(
        val messageId: String,
        val roomId: String,
        val senderId: String,
        val content: String,
        val messageType: MessageType,
        val createdAt: Instant,
        val clientMessageId: String,
        val expiresAt: Instant? = null,
    ) : ChatEvent()

    data class MessageRead(
        val roomId: String,
        val userId: String,
        val lastReadAt: Instant,
    ) : ChatEvent()

    data class UserConnected(
        val userId: String,
        val sessionId: String,
    ) : ChatEvent()

    data class UserDisconnected(
        val userId: String,
        val sessionId: String,
    ) : ChatEvent()

    data class RoomExpiring(
        val roomId: String,
        val roomName: String?,
        val expiresAt: Instant,
    ) : ChatEvent()

    data class RoomExpired(
        val roomId: String,
        val roomName: String?,
    ) : ChatEvent()

    data class MessageExpired(
        val roomId: String,
        val messageIds: List<String>,
    ) : ChatEvent()
}
