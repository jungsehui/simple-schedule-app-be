package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.user.domain.repository.UserRepository
import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.ChatRoom
import com.geekchat.server.domain.model.ChatRoomMember
import com.geekchat.server.domain.model.ChatRoomType
import com.geekchat.server.user.domain.model.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class RoomWithMembers(
    val room: ChatRoom,
    val members: List<MemberInfo>,
)

data class MemberInfo(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
)

@Service
class ChatRoomService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val userRepository: UserRepository,
    private val webSocketBroadcaster: WebSocketBroadcaster,
) {
    @Transactional
    fun setRoomMuted(userId: String, roomId: String, muted: Boolean): Either<ChatError, Unit> {
        val member = chatRoomMemberRepository.findByUserIdAndChatRoomId(userId, roomId)
            ?: return Either.Left(ChatError.NotRoomMember(userId, roomId))
        chatRoomMemberRepository.save(member.withMuted(muted))
        return Either.Right(Unit)
    }

    fun getRoomsForUser(userId: String): Either<ChatError, List<RoomWithMembers>> {
        val rooms = chatRoomRepository.findAllByUserId(userId)

        val result = rooms.map { room ->
            val members = chatRoomMemberRepository.findAllByChatRoomId(room.id)
            val memberInfos = members.mapNotNull { member ->
                userRepository.findById(member.userId)?.let { user ->
                    MemberInfo(
                        userId = user.id,
                        nickname = user.nickname,
                        profileImageUrl = user.profileImageUrl,
                    )
                }
            }
            RoomWithMembers(room = room, members = memberInfos)
        }

        return Either.Right(result)
    }

    @Transactional
    fun createRoom(
        userId: String,
        memberIds: List<String>,
        name: String?,
        ttlHours: Long? = null,
    ): Either<ChatError, RoomWithMembers> {
        return if (memberIds.size == 1 && name == null) {
            findOrCreateDirectRoom(userId, memberIds[0])
        } else {
            createGroupRoom(userId, memberIds, name, ttlHours)
        }
    }

    private fun findOrCreateDirectRoom(
        userId: String,
        otherUserId: String,
    ): Either<ChatError, RoomWithMembers> {
        userRepository.findById(otherUserId)
            ?: return Either.Left(ChatError.UserNotFound(otherUserId))

        val existingRoom = chatRoomRepository.findDirectRoomBetween(userId, otherUserId)
        if (existingRoom != null) {
            return loadRoomWithMembers(existingRoom)
        }

        val room = chatRoomRepository.save(
            ChatRoom(
                id = UUID.randomUUID().toString(),
                type = ChatRoomType.DIRECT,
            ),
        )

        addMember(room.id, userId)
        addMember(room.id, otherUserId)

        joinRoomForUsers(room.id, listOf(userId, otherUserId))
        return loadRoomWithMembers(room)
    }

    private fun createGroupRoom(
        userId: String,
        memberIds: List<String>,
        name: String?,
        ttlHours: Long? = null,
    ): Either<ChatError, RoomWithMembers> {
        val allMemberIds = (listOf(userId) + memberIds).distinct()

        for (memberId in allMemberIds) {
            if (memberId != userId) {
                userRepository.findById(memberId)
                    ?: return Either.Left(ChatError.UserNotFound(memberId))
            }
        }

        if (allMemberIds.size > ChatRoom.MAX_GROUP_MEMBERS) {
            return Either.Left(ChatError.RoomFull("", ChatRoom.MAX_GROUP_MEMBERS))
        }

        val expiresAt = ttlHours?.let { Instant.now().plus(it, ChronoUnit.HOURS) }

        val room = chatRoomRepository.save(
            ChatRoom(
                id = UUID.randomUUID().toString(),
                type = ChatRoomType.GROUP,
                name = name ?: "Group",
                expiresAt = expiresAt,
            ),
        )

        allMemberIds.forEach { memberId -> addMember(room.id, memberId) }

        joinRoomForUsers(room.id, allMemberIds)
        return loadRoomWithMembers(room)
    }

    private fun addMember(roomId: String, userId: String) {
        chatRoomMemberRepository.save(
            ChatRoomMember(
                id = UUID.randomUUID().toString(),
                userId = userId,
                chatRoomId = roomId,
            ),
        )
    }

    private fun joinRoomForUsers(roomId: String, userIds: List<String>) {
        userIds.forEach { userId -> webSocketBroadcaster.joinRoom(userId, roomId) }
    }

    private fun loadRoomWithMembers(room: ChatRoom): Either<ChatError, RoomWithMembers> {
        val members = chatRoomMemberRepository.findAllByChatRoomId(room.id)
        val memberInfos = members.mapNotNull { member ->
            userRepository.findById(member.userId)?.let { user ->
                MemberInfo(userId = user.id, nickname = user.nickname, profileImageUrl = user.profileImageUrl)
            }
        }
        return Either.Right(RoomWithMembers(room = room, members = memberInfos))
    }
}
