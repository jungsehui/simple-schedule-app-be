package com.geekchat.server.adapter.out.persistence.repository

import com.geekchat.server.adapter.out.persistence.entity.RefreshTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface SpringDataRefreshTokenRepository : JpaRepository<RefreshTokenJpaEntity, String> {
    fun findByToken(token: String): RefreshTokenJpaEntity?

    @Modifying
    fun deleteByToken(token: String)

    @Modifying
    fun deleteAllByUserId(userId: String)
}
