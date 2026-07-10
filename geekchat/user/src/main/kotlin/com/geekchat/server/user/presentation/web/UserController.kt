package com.geekchat.server.user.presentation.web

import com.geekchat.server.user.presentation.web.dto.SetUsernameRequest
import com.geekchat.server.user.presentation.web.dto.UserSearchResponse
import com.geekchat.server.common.presentation.web.toResponseEntity
import com.geekchat.server.user.application.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/search")
    fun searchUsers(
        @RequestParam q: String,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<*> {
        return userService.searchUsers(q, userId).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { users ->
                ResponseEntity.ok(users.map { UserSearchResponse.from(it) })
            },
        )
    }

    @PatchMapping("/me/username")
    fun setUsername(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: SetUsernameRequest,
    ): ResponseEntity<*> {
        return userService.setUsername(userId, request.username).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { user -> ResponseEntity.ok(mapOf("username" to user.username)) },
        )
    }
}
