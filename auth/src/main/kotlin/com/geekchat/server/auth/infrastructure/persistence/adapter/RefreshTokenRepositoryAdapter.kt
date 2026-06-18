package com.geekchat.server.auth.infrastructure.persistence.adapter

import com.geekchat.server.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity
import com.geekchat.server.auth.infrastructure.persistence.repository.SpringDataRefreshTokenRepository
import com.geekchat.server.user.infrastructure.persistence.repository.SpringDataUserRepository
import com.geekchat.server.auth.domain.repository.RefreshTokenRepository
import com.geekchat.server.auth.domain.model.RefreshToken
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class RefreshTokenRepositoryAdapter(
    private val jpaRepository: SpringDataRefreshTokenRepository,
    private val userJpaRepository: SpringDataUserRepository,
) : RefreshTokenRepository {

    override fun findByToken(token: String): RefreshToken? =
        jpaRepository.findByToken(token)?.toDomain()

    override fun save(refreshToken: RefreshToken): RefreshToken {
        val userEntity = userJpaRepository.getReferenceById(refreshToken.userId)
        return jpaRepository.save(RefreshTokenJpaEntity.fromDomain(refreshToken, userEntity)).toDomain()
    }

    @Transactional
    override fun deleteByToken(token: String) {
        jpaRepository.deleteByToken(token)
    }

    @Transactional
    override fun deleteAllByUserId(userId: String) {
        jpaRepository.deleteAllByUserId(userId)
    }
}
