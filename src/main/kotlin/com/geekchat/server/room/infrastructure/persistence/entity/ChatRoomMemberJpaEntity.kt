package com.geekchat.server.room.infrastructure.persistence.entity
import com.geekchat.server.user.infrastructure.persistence.entity.UserJpaEntity
import com.geekchat.server.common.infrastructure.persistence.entity.BaseJpaEntity

import com.geekchat.server.room.domain.model.ChatRoomMember
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "chat_room_member",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "chat_room_id"])],
)
class ChatRoomMemberJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserJpaEntity = UserJpaEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoomJpaEntity = ChatRoomJpaEntity(),

    @Column(name = "joined_at", nullable = false)
    var joinedAt: Instant = Instant.now(),

    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,

    @Column(nullable = false)
    var muted: Boolean = false,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) : BaseJpaEntity(id, createdAt, updatedAt) {

    fun toDomain(): ChatRoomMember = ChatRoomMember(
        id = id,
        userId = user.id,
        chatRoomId = chatRoom.id,
        joinedAt = joinedAt,
        lastReadAt = lastReadAt,
        muted = muted,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(
            domain: ChatRoomMember,
            userEntity: UserJpaEntity,
            chatRoomEntity: ChatRoomJpaEntity,
        ): ChatRoomMemberJpaEntity = ChatRoomMemberJpaEntity(
            id = domain.id,
            user = userEntity,
            chatRoom = chatRoomEntity,
            joinedAt = domain.joinedAt,
            lastReadAt = domain.lastReadAt,
            muted = domain.muted,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
    }
}
