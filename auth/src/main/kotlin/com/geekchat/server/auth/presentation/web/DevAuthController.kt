package com.geekchat.server.auth.presentation.web

import com.geekchat.server.common.presentation.web.ErrorResponse
import com.geekchat.server.common.presentation.web.toResponseEntity
import com.geekchat.server.auth.application.service.AuthService
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Dev-only auth shortcuts. Annotated [@Profile("dev", "local")] so the bean — and therefore
 * the `/auth/dev-login` endpoint — exists only in non-prod run modes (dev = integration tests,
 * local = local app run) and is absent from the production bean graph entirely
 * (stronger than a runtime profile check, which still ships the handler in the binary).
 */
@Profile("dev", "local")
@RestController
@RequestMapping("/auth")
class DevAuthController(
    private val authService: AuthService,
) {
    @GetMapping("/dev-login")
    fun devLogin(@RequestParam name: String?): ResponseEntity<*> {
        if (name.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(statusCode = 400, message = "name query parameter is required"),
            )
        }
        return authService.devLogin(name).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { result ->
                ResponseEntity.ok(
                    DevLoginResponse(result.accessToken, result.refreshToken, result.message),
                )
            },
        )
    }
}
