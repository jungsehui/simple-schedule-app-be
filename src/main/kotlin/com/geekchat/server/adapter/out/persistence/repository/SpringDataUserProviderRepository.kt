package com.geekchat.server.adapter.out.persistence.repository

import com.geekchat.server.adapter.out.persistence.entity.UserProviderJpaEntity
import com.geekchat.server.domain.model.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface SpringDataUserProviderRepository : JpaRepository<UserProviderJpaEntity, String> {
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): UserProviderJpaEntity?
    fun findFirstByEmail(email: String): UserProviderJpaEntity?
    fun findAllByUserId(userId: String): List<UserProviderJpaEntity>

    @Modifying
    fun deleteAllByUserId(userId: String)
}
