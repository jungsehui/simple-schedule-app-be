package com.geekchat.server.adapter.out.persistence.entity

import com.geekchat.server.domain.model.RefreshToken
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
    name = "refresh_token",
    uniqueConstraints = [UniqueConstraint(columnNames = ["token"])],
)
class RefreshTokenJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserJpaEntity = UserJpaEntity(),

    @Column(nullable = false, unique = true, length = 64)
    var token: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) : BaseJpaEntity(id, createdAt, updatedAt) {

    fun toDomain(): RefreshToken = RefreshToken(
        id = id,
        userId = user.id,
        token = token,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(domain: RefreshToken, userEntity: UserJpaEntity): RefreshTokenJpaEntity =
            RefreshTokenJpaEntity(
                id = domain.id,
                user = userEntity,
                token = domain.token,
                expiresAt = domain.expiresAt,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
