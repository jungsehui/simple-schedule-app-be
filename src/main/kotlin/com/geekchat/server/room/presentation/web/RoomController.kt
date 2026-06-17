package com.geekchat.server.room.presentation.web

import com.geekchat.server.common.presentation.web.toResponseEntity
import com.geekchat.server.room.application.service.ChatRoomService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rooms")
class RoomController(
    private val chatRoomService: ChatRoomService,
) {
    @GetMapping
    fun getRooms(@AuthenticationPrincipal userId: String): ResponseEntity<*> {
        return chatRoomService.getRoomsForUser(userId).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { rooms ->
                ResponseEntity.ok(rooms.map { RoomListResponse.from(it) })
            },
        )
    }

    @PostMapping
    fun createRoom(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: CreateRoomRequest,
    ): ResponseEntity<*> {
        return chatRoomService.createRoom(userId, request.memberIds, request.name, request.ttlHours).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { room -> ResponseEntity.ok(CreateRoomResponse.from(room)) },
        )
    }

    @PatchMapping("/{id}/mute")
    fun setMute(
        @AuthenticationPrincipal userId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: MuteRoomRequest,
    ): ResponseEntity<*> = chatRoomService.setRoomMuted(userId, id, request.muted).fold(
        onLeft = { it.toResponseEntity() },
        onRight = { ResponseEntity.ok(mapOf("muted" to request.muted)) },
    )
}
