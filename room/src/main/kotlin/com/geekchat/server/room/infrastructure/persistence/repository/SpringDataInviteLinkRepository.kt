package com.geekchat.server.room.infrastructure.persistence.repository

import com.geekchat.server.room.infrastructure.persistence.entity.InviteLinkJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataInviteLinkRepository : JpaRepository<InviteLinkJpaEntity, String> {
    fun findByCode(code: String): InviteLinkJpaEntity?
}
