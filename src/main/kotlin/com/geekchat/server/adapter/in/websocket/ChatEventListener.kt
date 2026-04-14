package com.geekchat.server.adapter.`in`.websocket

import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.domain.event.ChatEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ChatEventListener(
    private val broadcaster: WebSocketBroadcaster,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMessageSent(event: ChatEvent.MessageSent) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "new_message",
                data = mapOf(
                    "id" to event.messageId,
                    "roomId" to event.roomId,
                    "senderId" to event.senderId,
                    "content" to event.content,
                    "type" to event.messageType.name,
                    "createdAt" to event.createdAt.toString(),
                ),
            ),
        )

        log.info(
            "message_broadcast roomId={} senderId={} messageId={}",
            event.roomId, event.senderId, event.messageId,
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMessageRead(event: ChatEvent.MessageRead) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "read_update",
                data = mapOf(
                    "roomId" to event.roomId,
                    "userId" to event.userId,
                    "lastReadAt" to event.lastReadAt.toString(),
                ),
            ),
        )
    }
}
