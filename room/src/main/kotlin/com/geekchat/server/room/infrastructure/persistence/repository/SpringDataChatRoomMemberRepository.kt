package com.geekchat.server.room.infrastructure.persistence.repository

import com.geekchat.server.room.infrastructure.persistence.entity.ChatRoomMemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataChatRoomMemberRepository : JpaRepository<ChatRoomMemberJpaEntity, String> {
    fun findByUserIdAndChatRoomId(userId: String, chatRoomId: String): ChatRoomMemberJpaEntity?
    fun findAllByChatRoomId(chatRoomId: String): List<ChatRoomMemberJpaEntity>
    fun findAllByUserId(userId: String): List<ChatRoomMemberJpaEntity>
    fun existsByUserIdAndChatRoomId(userId: String, chatRoomId: String): Boolean
    fun deleteAllByChatRoomId(chatRoomId: String)
}
