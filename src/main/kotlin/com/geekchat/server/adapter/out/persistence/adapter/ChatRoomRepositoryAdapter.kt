package com.geekchat.server.adapter.out.persistence.adapter

import com.geekchat.server.adapter.out.persistence.entity.ChatRoomJpaEntity
import com.geekchat.server.adapter.out.persistence.entity.ChatRoomMemberJpaEntity
import com.geekchat.server.adapter.out.persistence.repository.SpringDataChatRoomMemberRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataChatRoomRepository
import com.geekchat.server.user.infrastructure.persistence.repository.SpringDataUserRepository
import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
class ChatRoomRepositoryAdapter(
    private val jpaRepository: SpringDataChatRoomRepository,
) : ChatRoomRepository {

    override fun findById(id: String): ChatRoom? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findDirectRoomBetween(userId1: String, userId2: String): ChatRoom? =
        jpaRepository.findDirectRoomBetween(userId1, userId2)?.toDomain()

    override fun findAllByUserId(userId: String): List<ChatRoom> =
        jpaRepository.findAllByUserId(userId).map { it.toDomain() }

    override fun save(chatRoom: ChatRoom): ChatRoom =
        jpaRepository.save(ChatRoomJpaEntity.fromDomain(chatRoom)).toDomain()

    override fun countMembers(roomId: String): Int =
        jpaRepository.countMembers(roomId)

    override fun findExpiredRooms(now: Instant): List<ChatRoom> =
        jpaRepository.findExpiredRooms(now).map { it.toDomain() }

    override fun findExpiringRoomsSoon(now: Instant, threshold: Instant): List<ChatRoom> =
        jpaRepository.findExpiringRoomsSoon(now, threshold).map { it.toDomain() }

    @Transactional
    override fun softDelete(roomId: String, now: Instant) {
        jpaRepository.softDeleteById(roomId, now)
    }
}

@Repository
class ChatRoomMemberRepositoryAdapter(
    private val jpaRepository: SpringDataChatRoomMemberRepository,
    private val userJpaRepository: SpringDataUserRepository,
    private val chatRoomJpaRepository: SpringDataChatRoomRepository,
) : ChatRoomMemberRepository {

    override fun findByUserIdAndChatRoomId(userId: String, chatRoomId: String): ChatRoomMember? =
        jpaRepository.findByUserIdAndChatRoomId(userId, chatRoomId)?.toDomain()

    override fun findAllByChatRoomId(chatRoomId: String): List<ChatRoomMember> =
        jpaRepository.findAllByChatRoomId(chatRoomId).map { it.toDomain() }

    override fun findAllByUserId(userId: String): List<ChatRoomMember> =
        jpaRepository.findAllByUserId(userId).map { it.toDomain() }

    override fun save(member: ChatRoomMember): ChatRoomMember {
        val userEntity = userJpaRepository.getReferenceById(member.userId)
        val chatRoomEntity = chatRoomJpaRepository.getReferenceById(member.chatRoomId)
        return jpaRepository.save(
            ChatRoomMemberJpaEntity.fromDomain(member, userEntity, chatRoomEntity),
        ).toDomain()
    }

    override fun existsByUserIdAndChatRoomId(userId: String, chatRoomId: String): Boolean =
        jpaRepository.existsByUserIdAndChatRoomId(userId, chatRoomId)

    @Transactional
    override fun deleteAllByChatRoomId(chatRoomId: String) {
        jpaRepository.deleteAllByChatRoomId(chatRoomId)
    }
}
