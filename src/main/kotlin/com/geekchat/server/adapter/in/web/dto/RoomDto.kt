package com.geekchat.server.adapter.`in`.web.dto

import com.geekchat.server.application.service.MemberInfo
import com.geekchat.server.application.service.RoomWithMembers
import jakarta.validation.constraints.NotEmpty

// --- Requests ---

data class CreateRoomRequest(
    @field:NotEmpty(message = "memberIds is required")
    val memberIds: List<String> = emptyList(),
    val name: String? = null,
    val ttlHours: Long? = null,
)

// --- Responses ---

data class RoomListResponse(
    val id: String,
    val type: String,
    val name: String?,
    val lastMessageAt: String?,
    val expiresAt: String?,
    val members: List<RoomMemberResponse>,
) {
    companion object {
        fun from(roomWithMembers: RoomWithMembers): RoomListResponse = RoomListResponse(
            id = roomWithMembers.room.id,
            type = roomWithMembers.room.type.name,
            name = roomWithMembers.room.name,
            lastMessageAt = roomWithMembers.room.lastMessageAt?.toString(),
            expiresAt = roomWithMembers.room.expiresAt?.toString(),
            members = roomWithMembers.members.map { RoomMemberResponse.from(it) },
        )
    }
}

data class RoomMemberResponse(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(info: MemberInfo): RoomMemberResponse = RoomMemberResponse(
            userId = info.userId,
            nickname = info.nickname,
            profileImageUrl = info.profileImageUrl,
        )
    }
}

data class CreateRoomResponse(
    val id: String,
    val type: String,
    val name: String?,
    val expiresAt: String?,
    val members: List<CreateRoomMemberResponse>,
) {
    companion object {
        fun from(roomWithMembers: RoomWithMembers): CreateRoomResponse = CreateRoomResponse(
            id = roomWithMembers.room.id,
            type = roomWithMembers.room.type.name,
            name = roomWithMembers.room.name,
            expiresAt = roomWithMembers.room.expiresAt?.toString(),
            members = roomWithMembers.members.map {
                CreateRoomMemberResponse(userId = it.userId, nickname = it.nickname)
            },
        )
    }
}

data class CreateRoomMemberResponse(
    val userId: String,
    val nickname: String,
)
