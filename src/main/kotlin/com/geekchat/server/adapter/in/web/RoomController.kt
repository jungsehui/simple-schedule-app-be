package com.geekchat.server.adapter.`in`.web

import com.geekchat.server.adapter.`in`.web.dto.CreateRoomRequest
import com.geekchat.server.adapter.`in`.web.dto.CreateRoomResponse
import com.geekchat.server.adapter.`in`.web.dto.MessageResponse
import com.geekchat.server.adapter.`in`.web.dto.RoomListResponse
import com.geekchat.server.adapter.`in`.web.dto.toResponseEntity
import com.geekchat.server.application.port.out.PaginationDirection
import com.geekchat.server.application.service.ChatRoomService
import com.geekchat.server.application.service.MessageService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/rooms")
class RoomController(
    private val chatRoomService: ChatRoomService,
    private val messageService: MessageService,
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
        return chatRoomService.createRoom(userId, request.memberIds, request.name).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { room -> ResponseEntity.ok(CreateRoomResponse.from(room)) },
        )
    }

    @GetMapping("/{id}/messages")
    fun getMessages(
        @AuthenticationPrincipal userId: String,
        @PathVariable id: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "backward") direction: String,
    ): ResponseEntity<*> {
        val cursorInstant = cursor?.let {
            try {
                Instant.parse(it)
            } catch (_: Exception) {
                return ResponseEntity.badRequest().body(
                    mapOf("statusCode" to 400, "message" to "Invalid cursor format"),
                )
            }
        }
        val paginationDirection = when (direction.lowercase()) {
            "forward" -> PaginationDirection.FORWARD
            else -> PaginationDirection.BACKWARD
        }

        return messageService.getMessages(id, userId, cursorInstant, limit, paginationDirection).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { messages ->
                ResponseEntity.ok(messages.map { MessageResponse.from(it) })
            },
        )
    }
}
