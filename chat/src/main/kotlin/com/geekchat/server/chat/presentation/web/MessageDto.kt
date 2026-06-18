package com.geekchat.server.chat.presentation.web

import com.geekchat.server.chat.application.service.MessageWithSender

data class MessageResponse(
    val id: String,
    val senderId: String,
    val senderNickname: String,
    val content: String,
    val type: String,
    val createdAt: String,
    val expiresAt: String? = null,
    val replyToMessageId: String? = null,
    val burnAfterRead: Boolean = false,
) {
    companion object {
        fun from(mws: MessageWithSender): MessageResponse = MessageResponse(
            id = mws.message.id,
            senderId = mws.message.senderId,
            senderNickname = mws.senderNickname,
            content = mws.message.content,
            type = mws.message.type.name,
            createdAt = mws.message.createdAt.toString(),
            expiresAt = mws.message.expiresAt?.toString(),
            replyToMessageId = mws.message.replyToMessageId,
            burnAfterRead = mws.message.burnAfterRead,
        )
    }
}
