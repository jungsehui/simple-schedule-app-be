package com.geekchat.server.adapter.`in`.web

import com.geekchat.server.adapter.`in`.web.dto.DevLoginResponse
import com.geekchat.server.adapter.`in`.web.dto.ErrorResponse
import com.geekchat.server.adapter.`in`.web.dto.LogoutRequest
import com.geekchat.server.adapter.`in`.web.dto.RefreshTokenRequest
import com.geekchat.server.adapter.`in`.web.dto.TokenPairResponse
import com.geekchat.server.adapter.`in`.web.dto.UserMeResponse
import com.geekchat.server.adapter.`in`.web.dto.toResponseEntity
import com.geekchat.server.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val environment: Environment,
) {
    @GetMapping("/dev-login")
    fun devLogin(@RequestParam name: String?): ResponseEntity<*> {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return ResponseEntity.notFound().build<Unit>()
        }
        if (name.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(statusCode = 400, message = "name query parameter is required"),
            )
        }
        return authService.devLogin(name).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { result ->
                ResponseEntity.ok(
                    DevLoginResponse(
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        message = result.message,
                    ),
                )
            },
        )
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<*> {
        return authService.refreshToken(request.refreshToken).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { pair ->
                ResponseEntity.ok(
                    TokenPairResponse(
                        accessToken = pair.accessToken,
                        refreshToken = pair.refreshToken,
                    ),
                )
            },
        )
    }

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal userId: String?): ResponseEntity<*> {
        if (userId == null) {
            return ResponseEntity.status(401).body(
                ErrorResponse(statusCode = 401, message = "Token not provided"),
            )
        }
        return authService.getCurrentUser(userId).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { user -> ResponseEntity.ok(UserMeResponse.from(user)) },
        )
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<*> {
        return authService.logout(request.refreshToken).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { ResponseEntity.ok(mapOf("success" to true)) },
        )
    }
}
