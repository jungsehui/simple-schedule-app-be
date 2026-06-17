package com.geekchat.server.adapter.`in`.web

import com.geekchat.server.adapter.`in`.web.dto.CreateInviteLinkRequest
import com.geekchat.server.adapter.`in`.web.dto.CreateRoomResponse
import com.geekchat.server.adapter.`in`.web.dto.InviteLinkResponse
import com.geekchat.server.common.presentation.web.toResponseEntity
import com.geekchat.server.application.service.InviteLinkService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class InviteLinkController(
    private val inviteLinkService: InviteLinkService,
) {
    @PostMapping("/api/rooms/{roomId}/invite-link")
    fun createInviteLink(
        @AuthenticationPrincipal userId: String,
        @PathVariable roomId: String,
        @RequestBody request: CreateInviteLinkRequest,
    ): ResponseEntity<*> {
        return inviteLinkService.createInviteLink(
            roomId = roomId,
            creatorId = userId,
            ttlHours = request.ttlHours ?: 24,
            maxUses = request.maxUses,
        ).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { link -> ResponseEntity.ok(InviteLinkResponse.from(link)) },
        )
    }

    @PostMapping("/api/invite/{code}/join")
    fun joinByInvite(
        @AuthenticationPrincipal userId: String,
        @PathVariable code: String,
    ): ResponseEntity<*> {
        return inviteLinkService.joinByInviteCode(code, userId).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { room -> ResponseEntity.ok(CreateRoomResponse.from(room)) },
        )
    }
}
