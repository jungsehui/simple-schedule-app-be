package com.geekchat.server.room.presentation.web

import com.geekchat.server.room.domain.model.InviteLink

data class CreateInviteLinkRequest(
    val ttlHours: Long? = 24,
    val maxUses: Int? = null,
)

data class InviteLinkResponse(
    val code: String,
    val roomId: String,
    val expiresAt: String,
    val maxUses: Int?,
    val currentUses: Int,
) {
    companion object {
        fun from(link: InviteLink): InviteLinkResponse = InviteLinkResponse(
            code = link.code,
            roomId = link.roomId,
            expiresAt = link.expiresAt.toString(),
            maxUses = link.maxUses,
            currentUses = link.currentUses,
        )
    }
}
