package com.geekchat.server.adapter.out.persistence.adapter

import com.geekchat.server.adapter.out.persistence.entity.InviteLinkJpaEntity
import com.geekchat.server.adapter.out.persistence.repository.SpringDataChatRoomRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataInviteLinkRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataUserRepository
import com.geekchat.server.application.port.out.InviteLinkRepository
import com.geekchat.server.domain.model.InviteLink
import org.springframework.stereotype.Repository

@Repository
class InviteLinkRepositoryAdapter(
    private val jpaRepository: SpringDataInviteLinkRepository,
    private val chatRoomJpaRepository: SpringDataChatRoomRepository,
    private val userJpaRepository: SpringDataUserRepository,
) : InviteLinkRepository {

    override fun findByCode(code: String): InviteLink? =
        jpaRepository.findByCode(code)?.toDomain()

    override fun save(inviteLink: InviteLink): InviteLink {
        val chatRoomEntity = chatRoomJpaRepository.getReferenceById(inviteLink.roomId)
        val creatorEntity = userJpaRepository.getReferenceById(inviteLink.creatorId)
        return jpaRepository.save(
            InviteLinkJpaEntity.fromDomain(inviteLink, chatRoomEntity, creatorEntity),
        ).toDomain()
    }
}
