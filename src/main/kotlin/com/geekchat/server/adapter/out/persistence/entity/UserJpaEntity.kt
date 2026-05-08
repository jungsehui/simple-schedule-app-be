package com.geekchat.server.adapter.out.persistence.entity

import com.geekchat.server.domain.model.User
import com.geekchat.server.domain.model.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Notes:
 * - Table renamed from `"user"` (PostgreSQL quoted) → `users` (MySQL friendly).
 * - `@SQLRestriction("deleted_at IS NULL")` removed: withdrawal uses `status` enum
 *   so that `@ManyToOne` joins from Message/ChatRoomMember don't break.
 * - `deleted_at` column inherited from SoftDeletableJpaEntity is kept for schema
 *   compatibility but not used.
 */
@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_user_username", columnList = "username")],
)
class UserJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, length = 20)
    var nickname: String = "",

    @Column(unique = true, length = 20)
    var username: String? = null,

    @Column
    var email: String? = null,

    @Column(name = "profile_image_url", length = 1024)
    var profileImageUrl: String? = null,

    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    deletedAt: Instant? = null,
) : SoftDeletableJpaEntity(id, createdAt, updatedAt, deletedAt) {

    fun toDomain(): User = User(
        id = id,
        nickname = nickname,
        username = username,
        email = email,
        profileImageUrl = profileImageUrl,
        passwordHash = passwordHash,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun fromDomain(user: User): UserJpaEntity = UserJpaEntity(
            id = user.id,
            nickname = user.nickname,
            username = user.username,
            email = user.email,
            profileImageUrl = user.profileImageUrl,
            passwordHash = user.passwordHash,
            status = user.status,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            deletedAt = user.deletedAt,
        )
    }
}
