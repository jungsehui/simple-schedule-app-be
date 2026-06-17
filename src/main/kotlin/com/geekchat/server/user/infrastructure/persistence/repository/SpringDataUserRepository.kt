package com.geekchat.server.user.infrastructure.persistence.repository

import com.geekchat.server.user.infrastructure.persistence.entity.UserJpaEntity
import com.geekchat.server.user.domain.model.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SpringDataUserRepository : JpaRepository<UserJpaEntity, String> {
    fun findByUsername(username: String): UserJpaEntity?

    fun findFirstByEmail(email: String): UserJpaEntity?

    @Query("SELECT u FROM UserJpaEntity u WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :query, '%')) AND u.id <> :excludeUserId AND u.status = 'ACTIVE'")
    fun searchByNickname(query: String, excludeUserId: String): List<UserJpaEntity>

    fun existsByUsername(username: String): Boolean

    fun existsByEmailAndStatus(email: String, status: UserStatus): Boolean
}
