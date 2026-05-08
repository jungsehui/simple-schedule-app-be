package com.geekchat.server.adapter.out.persistence.repository

import com.geekchat.server.adapter.out.persistence.entity.ChatRoomJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface SpringDataChatRoomRepository : JpaRepository<ChatRoomJpaEntity, String> {

    @Query(
        """
        SELECT cr FROM ChatRoomJpaEntity cr
        WHERE cr.type = 'DIRECT'
        AND EXISTS (SELECT 1 FROM ChatRoomMemberJpaEntity m1 WHERE m1.chatRoom = cr AND m1.user.id = :userId1)
        AND EXISTS (SELECT 1 FROM ChatRoomMemberJpaEntity m2 WHERE m2.chatRoom = cr AND m2.user.id = :userId2)
        """,
    )
    fun findDirectRoomBetween(userId1: String, userId2: String): ChatRoomJpaEntity?

    @Query(
        """
        SELECT cr FROM ChatRoomJpaEntity cr
        JOIN ChatRoomMemberJpaEntity m ON m.chatRoom = cr
        WHERE m.user.id = :userId
        ORDER BY COALESCE(cr.lastMessageAt, cr.createdAt) DESC
        """,
    )
    fun findAllByUserId(userId: String): List<ChatRoomJpaEntity>

    @Query("SELECT COUNT(m) FROM ChatRoomMemberJpaEntity m WHERE m.chatRoom.id = :roomId")
    fun countMembers(roomId: String): Int

    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE cr.expiresAt IS NOT NULL AND cr.expiresAt <= :now")
    fun findExpiredRooms(now: Instant): List<ChatRoomJpaEntity>

    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE cr.expiresAt IS NOT NULL AND cr.expiresAt > :now AND cr.expiresAt <= :threshold")
    fun findExpiringRoomsSoon(now: Instant, threshold: Instant): List<ChatRoomJpaEntity>

    @Modifying
    @Query("UPDATE ChatRoomJpaEntity cr SET cr.deletedAt = :now, cr.updatedAt = :now WHERE cr.id = :roomId AND cr.deletedAt IS NULL")
    fun softDeleteById(roomId: String, now: Instant)
}
