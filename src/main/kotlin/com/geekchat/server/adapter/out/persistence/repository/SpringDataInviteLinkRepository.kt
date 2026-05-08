package com.geekchat.server.adapter.out.persistence.repository

import com.geekchat.server.adapter.out.persistence.entity.InviteLinkJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataInviteLinkRepository : JpaRepository<InviteLinkJpaEntity, String> {
    fun findByCode(code: String): InviteLinkJpaEntity?
}
