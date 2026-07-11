package com.geekchat.server.user.infrastructure.persistence.entity
import com.geekchat.server.common.infrastructure.persistence.entity.BaseJpaEntity

import com.geekchat.server.user.domain.model.AuthProvider
import com.geekchat.server.user.domain.model.UserProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "user_provider",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
class UserProviderJpaEntity(
    id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserJpaEntity = UserJpaEntity(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: AuthProvider = AuthProvider.GOOGLE,

    @Column(name = "provider_id", nullable = false)
    var providerId: String = "",

    @Column
    var email: String? = null,

    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) : BaseJpaEntity(id, createdAt, updatedAt) {

    fun toDomain(): UserProvider = UserProvider(
        id = id,
        userId = user.id,
        provider = provider,
        providerId = providerId,
        email = email,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(domain: UserProvider, userEntity: UserJpaEntity): UserProviderJpaEntity =
            UserProviderJpaEntity(
                id = domain.id,
                user = userEntity,
                provider = domain.provider,
                providerId = domain.providerId,
                email = domain.email,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
