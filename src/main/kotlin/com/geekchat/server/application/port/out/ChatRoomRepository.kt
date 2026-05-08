package com.geekchat.server.application.port.out

import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import java.time.Instant

interface ChatRoomRepository {
    fun findById(id: String): ChatRoom?
    fun findDirectRoomBetween(userId1: String, userId2: String): ChatRoom?
    fun findAllByUserId(userId: String): List<ChatRoom>
    fun save(chatRoom: ChatRoom): ChatRoom
    fun countMembers(roomId: String): Int
    fun findExpiredRooms(now: Instant): List<ChatRoom>
    fun findExpiringRoomsSoon(now: Instant, threshold: Instant): List<ChatRoom>
    fun softDelete(roomId: String, now: Instant)
}

interface ChatRoomMemberRepository {
    fun findByUserIdAndChatRoomId(userId: String, chatRoomId: String): ChatRoomMember?
    fun findAllByChatRoomId(chatRoomId: String): List<ChatRoomMember>
    fun findAllByUserId(userId: String): List<ChatRoomMember>
    fun save(member: ChatRoomMember): ChatRoomMember
    fun existsByUserIdAndChatRoomId(userId: String, chatRoomId: String): Boolean
    fun deleteAllByChatRoomId(chatRoomId: String)
}
