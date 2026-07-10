package com.geekchat.server.chat.application

import com.geekchat.server.chat.domain.repository.MessageRepository
import com.geekchat.server.room.domain.event.RoomExpired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant

/**
 * Chat-side reaction to a room expiring: soft-delete that room's messages.
 * Message lifecycle is the chat module's responsibility, so the room module
 * publishes RoomExpired and chat handles cleanup — avoids a room→chat dependency
 * (Spring Modulith inter-module communication via events).
 */
@Component
class RoomExpiryMessageCleanup(
    private val messageRepository: MessageRepository,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRoomExpired(event: RoomExpired) {
        messageRepository.softDeleteByRoomId(event.roomId, Instant.now())
    }
}
