package com.geekchat.server.adapter.out.persistence.entity

import com.geekchat.server.domain.model.Message
import com.geekchat.server.domain.model.MessageType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "message",
    uniqueConstraints = [UniqueConstraint(columnNames = ["client_message_id"])],
    indexes = [
        Index(name = "idx_message_room_created", columnList = "chat_room_id, created_at DESC"),
        Index(name = "idx_message_expires_at", columnList = "expires_at"),
        Index(name = "idx_message_reply_to", columnList = "reply_to_message_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class MessageJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoomJpaEntity = ChatRoomJpaEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: UserJpaEntity = UserJpaEntity(),

    @Column(name = "client_message_id", nullable = false, unique = true)
    var clientMessageId: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: MessageType = MessageType.TEXT,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "reply_to_message_id")
    var replyToMessageId: String? = null,

    @Column(name = "burn_after_read", nullable = false)
    var burnAfterRead: Boolean = false,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    deletedAt: Instant? = null,
) : SoftDeletableJpaEntity(id, createdAt, updatedAt, deletedAt) {

    fun toDomain(): Message = Message(
        id = id,
        chatRoomId = chatRoom.id,
        senderId = sender.id,
        clientMessageId = clientMessageId,
        content = content,
        type = type,
        expiresAt = expiresAt,
        replyToMessageId = replyToMessageId,
        burnAfterRead = burnAfterRead,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun fromDomain(
            domain: Message,
            chatRoomEntity: ChatRoomJpaEntity,
            senderEntity: UserJpaEntity,
        ): MessageJpaEntity = MessageJpaEntity(
            id = domain.id,
            chatRoom = chatRoomEntity,
            sender = senderEntity,
            clientMessageId = domain.clientMessageId,
            content = domain.content,
            type = domain.type,
            expiresAt = domain.expiresAt,
            replyToMessageId = domain.replyToMessageId,
            burnAfterRead = domain.burnAfterRead,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            deletedAt = domain.deletedAt,
        )
    }
}
