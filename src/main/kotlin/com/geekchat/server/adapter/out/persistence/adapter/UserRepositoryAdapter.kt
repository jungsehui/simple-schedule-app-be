package com.geekchat.server.adapter.out.persistence.adapter

import com.geekchat.server.adapter.out.persistence.entity.UserJpaEntity
import com.geekchat.server.adapter.out.persistence.entity.UserProviderJpaEntity
import com.geekchat.server.adapter.out.persistence.repository.SpringDataUserProviderRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataUserRepository
import com.geekchat.server.application.port.out.UserProviderRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.domain.model.AuthProvider
import com.geekchat.server.domain.model.User
import com.geekchat.server.domain.model.UserProvider
import com.geekchat.server.domain.model.UserStatus
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: SpringDataUserRepository,
) : UserRepository {

    override fun findById(id: String): User? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findActiveById(id: String): User? =
        jpaRepository.findById(id).orElse(null)
            ?.takeIf { it.status == UserStatus.ACTIVE }
            ?.toDomain()

    override fun findByUsername(username: String): User? =
        jpaRepository.findByUsername(username)?.toDomain()

    override fun findByEmail(email: String): User? =
        jpaRepository.findFirstByEmail(email)?.toDomain()

    override fun searchByNickname(query: String, excludeUserId: String, limit: Int): List<User> =
        jpaRepository.searchByNickname(query, excludeUserId)
            .take(limit)
            .map { it.toDomain() }

    override fun searchByUsername(username: String, excludeUserId: String): User? =
        jpaRepository.findByUsername(username)
            ?.takeIf { it.id != excludeUserId && it.status == UserStatus.ACTIVE }
            ?.toDomain()

    override fun save(user: User): User =
        jpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain()

    override fun existsByUsername(username: String): Boolean =
        jpaRepository.existsByUsername(username)

    override fun existsByEmailAndStatusActive(email: String): Boolean =
        jpaRepository.existsByEmailAndStatus(email, UserStatus.ACTIVE)
}

@Repository
class UserProviderRepositoryAdapter(
    private val jpaRepository: SpringDataUserProviderRepository,
    private val userJpaRepository: SpringDataUserRepository,
) : UserProviderRepository {

    override fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): UserProvider? =
        jpaRepository.findByProviderAndProviderId(provider, providerId)?.toDomain()

    override fun findByEmail(email: String): UserProvider? =
        jpaRepository.findFirstByEmail(email)?.toDomain()

    override fun findAllByUserId(userId: String): List<UserProvider> =
        jpaRepository.findAllByUserId(userId).map { it.toDomain() }

    override fun save(userProvider: UserProvider): UserProvider {
        val userEntity = userJpaRepository.getReferenceById(userProvider.userId)
        return jpaRepository.save(UserProviderJpaEntity.fromDomain(userProvider, userEntity)).toDomain()
    }

    @Transactional
    override fun deleteAllByUserId(userId: String) {
        jpaRepository.deleteAllByUserId(userId)
    }
}
