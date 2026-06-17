package com.geekchat.server.websocket.presentation
import com.geekchat.server.room.domain.event.*
import com.geekchat.server.chat.domain.event.*

import com.geekchat.server.websocket.application.port.out.WebSocketBroadcaster
import com.geekchat.server.room.domain.event.RoomMemberJoined
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

    /**
     * Register the user's live sessions with the room on join. Synchronous (plain
     * @EventListener, runs inline at the publish point) so timing matches the former
     * direct ChatRoomService/InviteLinkService -> broadcaster.joinRoom call. This is
     * in-memory session bookkeeping with no transaction-commit ordering constraint.
     */
    @EventListener
    fun onRoomMemberJoined(event: RoomMemberJoined) {
        broadcaster.joinRoom(event.userId, event.roomId)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMessageSent(event: MessageSent) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "new_message",
                data = mapOf(
                    "id" to event.messageId,
                    "roomId" to event.roomId,
                    "senderId" to event.senderId,
                    "content" to event.content, // message body — delivered to room members (never logged)
                    "type" to event.messageType.name,
                    "createdAt" to event.createdAt.toString(),
                    "expiresAt" to event.expiresAt?.toString(),
                    "replyToMessageId" to event.replyToMessageId,
                    "burnAfterRead" to event.burnAfterRead,
                ),
            ),
        )

        log.info(
            "message_broadcast roomId={} senderId={} messageId={}",
            event.roomId, event.senderId, event.messageId,
        )
    }

    /** Burn-on-Read fires once when a non-sender reads the message → hard-deleted. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMessageBurned(event: MessageBurned) {
        broadcaster.broadcastToRoom(
            roomId = event.roomId,
            message = WsOutMessage(
                type = "message_burned",
                data = mapOf("roomId" to event.roomId, "messageId" to event.messageId),
            ),
        )
        log.info("message_burned_broadcast roomId={} messageId={}", event.roomId, event.messageId)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMessageRead(event: MessageRead) {
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
    fun onRoomExpiring(event: RoomExpiring) {
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
    fun onRoomExpired(event: RoomExpired) {
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
    fun onMessageExpired(event: MessageExpired) {
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
