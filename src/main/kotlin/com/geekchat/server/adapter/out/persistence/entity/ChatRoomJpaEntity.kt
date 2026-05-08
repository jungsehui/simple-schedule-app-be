package com.geekchat.server.adapter.out.persistence.entity

import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chat_room")
@SQLRestriction("deleted_at IS NULL")
class ChatRoomJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ChatRoomType = ChatRoomType.DIRECT,

    @Column
    var name: String? = null,

    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    deletedAt: Instant? = null,
) : SoftDeletableJpaEntity(id, createdAt, updatedAt, deletedAt) {

    fun toDomain(): ChatRoom = ChatRoom(
        id = id,
        type = type,
        name = name,
        lastMessageAt = lastMessageAt,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun fromDomain(domain: ChatRoom): ChatRoomJpaEntity = ChatRoomJpaEntity(
            id = domain.id,
            type = domain.type,
            name = domain.name,
            lastMessageAt = domain.lastMessageAt,
            expiresAt = domain.expiresAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            deletedAt = domain.deletedAt,
        )
    }
}
