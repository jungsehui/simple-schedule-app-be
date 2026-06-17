package com.geekchat.server.room.infrastructure.persistence.entity
import com.geekchat.server.user.infrastructure.persistence.entity.UserJpaEntity
import com.geekchat.server.common.infrastructure.persistence.entity.BaseJpaEntity

import com.geekchat.server.room.domain.model.InviteLink
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "invite_link",
    uniqueConstraints = [UniqueConstraint(columnNames = ["code"])],
    indexes = [Index(name = "idx_invite_link_code", columnList = "code")],
)
class InviteLinkJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, unique = true, length = 8)
    var code: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var chatRoom: ChatRoomJpaEntity = ChatRoomJpaEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    var creator: UserJpaEntity = UserJpaEntity(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "max_uses")
    var maxUses: Int? = null,

    @Column(name = "current_uses", nullable = false)
    var currentUses: Int = 0,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) : BaseJpaEntity(id, createdAt, updatedAt) {

    fun toDomain(): InviteLink = InviteLink(
        id = id,
        code = code,
        roomId = chatRoom.id,
        creatorId = creator.id,
        expiresAt = expiresAt,
        maxUses = maxUses,
        currentUses = currentUses,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(
            domain: InviteLink,
            chatRoomEntity: ChatRoomJpaEntity,
            creatorEntity: UserJpaEntity,
        ): InviteLinkJpaEntity = InviteLinkJpaEntity(
            id = domain.id,
            code = domain.code,
            chatRoom = chatRoomEntity,
            creator = creatorEntity,
            expiresAt = domain.expiresAt,
            maxUses = domain.maxUses,
            currentUses = domain.currentUses,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
    }
}
