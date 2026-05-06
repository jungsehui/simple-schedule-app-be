package com.geekchat.server.adapter.`in`.websocket

import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.domain.event.ChatEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
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
                    "content" to event.content, // TODO: Remove content from log before production
                    "type" to event.messageType.name,
                    "createdAt" to event.createdAt.toString(),
                    "expiresAt" to event.expiresAt?.toString(),
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

    @Async
    @EventListener
    fun onRoomExpiring(event: ChatEvent.RoomExpiring) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "room_expiring",
                data = mapOf(
                    "roomId" to event.roomId,
                    "roomName" to event.roomName,
                    "expiresAt" to event.expiresAt.toString(),
                ),
            ),
        )
        log.info("room_expiring_broadcast roomId={} expiresAt={}", event.roomId, event.expiresAt)
    }

    @Async
    @EventListener
    fun onRoomExpired(event: ChatEvent.RoomExpired) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "room_expired",
                data = mapOf("roomId" to event.roomId, "roomName" to event.roomName),
            ),
        )
        log.info("room_expired_broadcast roomId={}", event.roomId)
    }

    @Async
    @EventListener
    fun onMessageExpired(event: ChatEvent.MessageExpired) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "message_expired",
                data = mapOf("roomId" to event.roomId, "messageIds" to event.messageIds),
            ),
        )
        log.info("message_expired_broadcast roomId={} count={}", event.roomId, event.messageIds.size)
    }
}
