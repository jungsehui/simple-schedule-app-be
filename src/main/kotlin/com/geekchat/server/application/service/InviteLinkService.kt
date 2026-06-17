package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.port.out.ChatRoomRepository
import com.geekchat.server.application.port.out.InviteLinkRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.ChatRoomMember
import com.geekchat.server.domain.model.InviteLink
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class InviteLinkService(
    private val inviteLinkRepository: InviteLinkRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val userRepository: UserRepository,
    private val webSocketBroadcaster: WebSocketBroadcaster,
) {
    @Transactional
    fun createInviteLink(
        roomId: String,
        creatorId: String,
        ttlHours: Long = 24,
        maxUses: Int? = null,
    ): Either<ChatError, InviteLink> {
        chatRoomRepository.findById(roomId)
            ?: return Either.Left(ChatError.RoomNotFound(roomId))

        if (!chatRoomMemberRepository.existsByUserIdAndChatRoomId(creatorId, roomId)) {
            return Either.Left(ChatError.NotRoomMember(creatorId, roomId))
        }

        val link = InviteLink(
            id = UUID.randomUUID().toString(),
            code = InviteLink.generateCode(),
            roomId = roomId,
            creatorId = creatorId,
            expiresAt = Instant.now().plus(ttlHours, ChronoUnit.HOURS),
            maxUses = maxUses,
        )

        return Either.Right(inviteLinkRepository.save(link))
    }

    @Transactional
    fun joinByInviteCode(code: String, userId: String): Either<ChatError, RoomWithMembers> {
        val link = inviteLinkRepository.findByCode(code)
            ?: return Either.Left(ChatError.InviteLinkNotFound(code))

        if (link.isExpired()) {
            return Either.Left(ChatError.InviteLinkExpired(code))
        }

        if (link.isMaxUsesReached()) {
            return Either.Left(ChatError.InviteLinkMaxUsesReached(code))
        }

        if (chatRoomMemberRepository.existsByUserIdAndChatRoomId(userId, link.roomId)) {
            return Either.Left(ChatError.AlreadyRoomMember(userId, link.roomId))
        }

        val room = chatRoomRepository.findById(link.roomId)
            ?: return Either.Left(ChatError.RoomNotFound(link.roomId))

        val memberCount = chatRoomRepository.countMembers(link.roomId)
        if (memberCount >= room.maxMembers) {
            return Either.Left(ChatError.RoomFull(link.roomId, room.maxMembers))
        }

        chatRoomMemberRepository.save(
            ChatRoomMember(
                id = UUID.randomUUID().toString(),
                userId = userId,
                chatRoomId = link.roomId,
            ),
        )

        inviteLinkRepository.save(link.withIncrementedUses())
        webSocketBroadcaster.joinRoom(userId, link.roomId)

        val members = chatRoomMemberRepository.findAllByChatRoomId(room.id)
        val memberInfos = members.mapNotNull { member ->
            userRepository.findById(member.userId)?.let { user ->
                MemberInfo(userId = user.id, nickname = user.nickname, profileImageUrl = user.profileImageUrl)
            }
        }
        return Either.Right(RoomWithMembers(room = room, members = memberInfos))
    }
}
