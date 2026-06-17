package com.geekchat.server.chat.presentation.web

import com.geekchat.server.chat.application.service.MessageService
import com.geekchat.server.chat.domain.repository.PaginationDirection
import com.geekchat.server.common.presentation.web.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/rooms")
class MessageController(
    private val messageService: MessageService,
) {
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
