package com.geekchat.server.room.infrastructure.persistence.adapter

import com.geekchat.server.room.infrastructure.persistence.entity.InviteLinkJpaEntity
import com.geekchat.server.room.infrastructure.persistence.repository.SpringDataChatRoomRepository
import com.geekchat.server.room.infrastructure.persistence.repository.SpringDataInviteLinkRepository
import com.geekchat.server.user.infrastructure.persistence.repository.SpringDataUserRepository
import com.geekchat.server.room.domain.repository.InviteLinkRepository
import com.geekchat.server.room.domain.model.InviteLink
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
