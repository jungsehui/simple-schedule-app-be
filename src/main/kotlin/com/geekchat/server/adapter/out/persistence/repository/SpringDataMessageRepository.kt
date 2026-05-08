package com.geekchat.server.adapter.out.persistence.repository

import com.geekchat.server.adapter.out.persistence.entity.MessageJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface SpringDataMessageRepository : JpaRepository<MessageJpaEntity, String> {
    fun findByClientMessageId(clientMessageId: String): MessageJpaEntity?

    @Query(
        """
        SELECT m FROM MessageJpaEntity m
        WHERE m.chatRoom.id = :roomId AND m.createdAt < :cursor
        ORDER BY m.createdAt DESC
        """,
    )
    fun findByRoomIdBefore(roomId: String, cursor: Instant, pageable: Pageable): List<MessageJpaEntity>

    @Query(
        """
        SELECT m FROM MessageJpaEntity m
        WHERE m.chatRoom.id = :roomId AND m.createdAt > :cursor
        ORDER BY m.createdAt ASC
        """,
    )
    fun findByRoomIdAfter(roomId: String, cursor: Instant, pageable: Pageable): List<MessageJpaEntity>

    @Query(
        """
        SELECT m FROM MessageJpaEntity m
        WHERE m.chatRoom.id = :roomId
        ORDER BY m.createdAt DESC
        """,
    )
    fun findByRoomIdLatest(roomId: String, pageable: Pageable): List<MessageJpaEntity>

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.expiresAt IS NOT NULL AND m.expiresAt <= :now")
    fun findExpiredMessages(now: Instant): List<MessageJpaEntity>

    @Modifying
    @Query("DELETE FROM MessageJpaEntity m WHERE m.id IN :ids")
    fun hardDeleteByIds(ids: List<String>)

    @Modifying
    @Query("UPDATE MessageJpaEntity m SET m.deletedAt = :now, m.updatedAt = :now WHERE m.chatRoom.id = :roomId AND m.deletedAt IS NULL")
    fun softDeleteByRoomId(roomId: String, now: Instant)
}
