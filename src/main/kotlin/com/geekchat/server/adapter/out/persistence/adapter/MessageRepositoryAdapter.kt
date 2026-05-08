package com.geekchat.server.adapter.out.persistence.adapter

import com.geekchat.server.adapter.out.persistence.entity.MessageJpaEntity
import com.geekchat.server.adapter.out.persistence.repository.SpringDataChatRoomRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataMessageRepository
import com.geekchat.server.adapter.out.persistence.repository.SpringDataUserRepository
import com.geekchat.server.application.port.out.MessageRepository
import com.geekchat.server.application.port.out.PaginationDirection
import com.geekchat.server.domain.model.Message
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
class MessageRepositoryAdapter(
    private val jpaRepository: SpringDataMessageRepository,
    private val chatRoomJpaRepository: SpringDataChatRoomRepository,
    private val userJpaRepository: SpringDataUserRepository,
) : MessageRepository {

    override fun findById(id: String): Message? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByClientMessageId(clientMessageId: String): Message? =
        jpaRepository.findByClientMessageId(clientMessageId)?.toDomain()

    override fun findByRoomId(
        roomId: String,
        cursor: Instant?,
        limit: Int,
        direction: PaginationDirection,
    ): List<Message> {
        val pageable = PageRequest.of(0, limit)
        val entities = if (cursor == null) {
            jpaRepository.findByRoomIdLatest(roomId, pageable)
        } else {
            when (direction) {
                PaginationDirection.BACKWARD -> jpaRepository.findByRoomIdBefore(roomId, cursor, pageable)
                PaginationDirection.FORWARD -> jpaRepository.findByRoomIdAfter(roomId, cursor, pageable)
            }
        }
        return entities.map { it.toDomain() }
    }

    override fun save(message: Message): Message {
        val chatRoomEntity = chatRoomJpaRepository.getReferenceById(message.chatRoomId)
        val senderEntity = userJpaRepository.getReferenceById(message.senderId)
        return jpaRepository.save(
            MessageJpaEntity.fromDomain(message, chatRoomEntity, senderEntity),
        ).toDomain()
    }

    override fun findExpiredMessages(now: Instant): List<Message> =
        jpaRepository.findExpiredMessages(now).map { it.toDomain() }

    @Transactional
    override fun hardDeleteByIds(ids: List<String>) {
        if (ids.isNotEmpty()) {
            ids.chunked(100).forEach { batch -> jpaRepository.hardDeleteByIds(batch) }
        }
    }

    @Transactional
    override fun softDeleteByRoomId(roomId: String, now: Instant) {
        jpaRepository.softDeleteByRoomId(roomId, now)
    }
}
