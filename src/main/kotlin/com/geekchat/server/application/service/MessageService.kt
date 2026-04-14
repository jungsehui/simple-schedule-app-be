package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.application.port.out.MessageRepository
import com.geekchat.server.application.port.out.PaginationDirection
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.domain.error.ChatError
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.event.ChatEvent
import com.geekchat.server.domain.model.Message
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class MessageWithSender(
    val message: Message,
    val senderNickname: String,
)

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        clientMessageId: String,
    ): Either<ChatError, Message> {
        // Idempotency: check memory/DB first
        messageRepository.findByClientMessageId(clientMessageId)?.let {
            return Either.Right(it)
        }

        if (!chatRoomMemberRepository.existsByUserIdAndChatRoomId(senderId, roomId)) {
            return Either.Left(ChatError.NotRoomMember(senderId, roomId))
        }

        if (content.isBlank()) {
            return Either.Left(ChatError.EmptyMessage())
        }

        val message = Message(
            id = UUID.randomUUID().toString(),
            chatRoomId = roomId,
            senderId = senderId,
            clientMessageId = clientMessageId,
            content = content,
        )

        val saved = try {
            messageRepository.save(message)
        } catch (_: DataIntegrityViolationException) {
            // Race condition: another thread inserted the same clientMessageId
            log.warn("Race condition caught: clientMessageId={}", clientMessageId)
            return Either.Right(
                messageRepository.findByClientMessageId(clientMessageId)
                    ?: return Either.Left(ChatError.Internal()),
            )
        }

        // Update room's lastMessageAt
        chatRoomRepository.findById(roomId)?.let { room ->
            chatRoomRepository.save(room.withLastMessageAt(saved.createdAt))
        }

        // Publish domain event (broadcast happens AFTER_COMMIT)
        eventPublisher.publishEvent(
            ChatEvent.MessageSent(
                messageId = saved.id,
                roomId = roomId,
                senderId = senderId,
                content = saved.content,
                messageType = saved.type,
                createdAt = saved.createdAt,
                clientMessageId = clientMessageId,
            ),
        )

        log.info(
            "message_sent roomId={} senderId={} clientMessageId={} messageId={}",
            roomId, senderId, clientMessageId, saved.id,
        )

        return Either.Right(saved)
    }

    @Transactional
    fun markAsRead(
        roomId: String,
        userId: String,
        lastReadMessageId: String,
    ): Either<ChatError, Unit> {
        val message = messageRepository.findById(lastReadMessageId)
            ?: return Either.Left(ChatError.MessageNotFound(lastReadMessageId))

        val member = chatRoomMemberRepository.findByUserIdAndChatRoomId(userId, roomId)
            ?: return Either.Left(ChatError.NotRoomMember(userId, roomId))

        val updated = member.withMarkReadAt(message.createdAt)
        if (updated !== member) {
            chatRoomMemberRepository.save(updated)

            eventPublisher.publishEvent(
                ChatEvent.MessageRead(
                    roomId = roomId,
                    userId = userId,
                    lastReadAt = message.createdAt,
                ),
            )
        }

        return Either.Right(Unit)
    }

    fun getMessages(
        roomId: String,
        userId: String,
        cursor: Instant?,
        limit: Int,
        direction: PaginationDirection,
    ): Either<ChatError, List<MessageWithSender>> {
        chatRoomRepository.findById(roomId)
            ?: return Either.Left(ChatError.RoomNotFound(roomId))

        if (!chatRoomMemberRepository.existsByUserIdAndChatRoomId(userId, roomId)) {
            return Either.Left(ChatError.NotRoomMember(userId, roomId))
        }

        val cappedLimit = limit.coerceIn(1, 100)

        val messages = messageRepository.findByRoomId(
            roomId = roomId,
            cursor = cursor,
            limit = cappedLimit,
            direction = direction,
        )

        val senderCache = mutableMapOf<String, String>()
        val result = messages.map { message ->
            val nickname = senderCache.getOrPut(message.senderId) {
                userRepository.findById(message.senderId)?.nickname ?: "Unknown"
            }
            MessageWithSender(message = message, senderNickname = nickname)
        }

        return Either.Right(result)
    }
}
