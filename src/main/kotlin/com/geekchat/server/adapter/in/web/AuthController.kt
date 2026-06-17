package com.geekchat.server.adapter.`in`.web

import com.geekchat.server.adapter.`in`.web.dto.CompleteOAuthSignupRequest
import com.geekchat.server.common.presentation.web.ErrorResponse
import com.geekchat.server.adapter.`in`.web.dto.LinkProviderRequest
import com.geekchat.server.adapter.`in`.web.dto.LoginRequest
import com.geekchat.server.adapter.`in`.web.dto.LogoutRequest
import com.geekchat.server.adapter.`in`.web.dto.RefreshTokenRequest
import com.geekchat.server.adapter.`in`.web.dto.SignupRequest
import com.geekchat.server.adapter.`in`.web.dto.SignupTokenResponse
import com.geekchat.server.adapter.`in`.web.dto.TokenPairResponse
import com.geekchat.server.adapter.`in`.web.dto.UserMeResponse
import com.geekchat.server.common.presentation.web.toResponseEntity
import com.geekchat.server.application.service.AuthService
import com.geekchat.server.application.service.LinkProviderResult
import com.geekchat.server.application.service.OAuthCallbackResult
import com.geekchat.server.application.service.SignupCommand
import com.geekchat.server.user.domain.model.AuthProvider
import com.geekchat.server.infrastructure.config.AppProperties
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val appProperties: AppProperties,
) {
    // ───────────── Local (ID/PW) ─────────────

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<*> {
        val cmd = SignupCommand(
            username = request.username,
            password = request.password,
            nickname = request.nickname,
            email = request.email,
        )
        return authService.signup(cmd).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { pair -> ResponseEntity.ok(TokenPairResponse(pair.accessToken, pair.refreshToken)) },
        )
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<*> {
        return authService.login(request.username, request.password).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { pair -> ResponseEntity.ok(TokenPairResponse(pair.accessToken, pair.refreshToken)) },
        )
    }

    @PostMapping("/withdraw")
    fun withdraw(@AuthenticationPrincipal userId: String?): ResponseEntity<*> {
        if (userId == null) {
            return ResponseEntity.status(401).body(ErrorResponse(401, "Token not provided"))
        }
        return authService.withdraw(userId).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { ResponseEntity.ok(mapOf("success" to true)) },
        )
    }

    // ───────────── OAuth ─────────────

    @GetMapping("/google")
    fun googleStart(): ResponseEntity<Void> {
        val cfg = appProperties.oauth.google
        val url = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", cfg.clientId)
            .queryParam("redirect_uri", appProperties.oauth.callbackUrl)
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email profile")
            .queryParam("state", "google")
            .build(true).toUriString()
        return redirect(url)
    }

    @GetMapping("/naver")
    fun naverStart(): ResponseEntity<Void> {
        val cfg = appProperties.oauth.naver
        val url = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/authorize")
            .queryParam("client_id", cfg.clientId)
            .queryParam("redirect_uri", appProperties.oauth.callbackUrl)
            .queryParam("response_type", "code")
            .queryParam("state", "naver")
            .build(true).toUriString()
        return redirect(url)
    }

    @GetMapping("/callback")
    fun oauthCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
    ): ResponseEntity<Void> {
        if (!error.isNullOrBlank()) {
            return redirect("${appProperties.frontendUrl}/auth/error?error=oauth_cancelled")
        }
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            return redirect("${appProperties.frontendUrl}/auth/error?error=missing_params")
        }
        val provider = when (state.lowercase()) {
            "google" -> AuthProvider.GOOGLE
            "naver" -> AuthProvider.NAVER
            else -> return redirect("${appProperties.frontendUrl}/auth/error?error=invalid_state")
        }

        return authService.handleOAuthCode(provider, code).fold(
            onLeft = { redirect("${appProperties.frontendUrl}/auth/error?error=oauth_failed") },
            onRight = { result ->
                val target = when (result) {
                    is OAuthCallbackResult.LoggedIn ->
                        "${appProperties.frontendUrl}/auth/success" +
                            "#access_token=${result.tokens.accessToken}&refresh_token=${result.tokens.refreshToken}"
                    is OAuthCallbackResult.LinkingRequired ->
                        "${appProperties.frontendUrl}/auth/oauth-link" +
                            "#link_token=${result.linkToken}" +
                            "&existing_nickname=${enc(result.existingNickname)}" +
                            "&new_provider=${result.newProvider.name}"
                    is OAuthCallbackResult.SignupRequired ->
                        "${appProperties.frontendUrl}/auth/oauth-complete" +
                            "#signup_token=${result.signupToken}" +
                            "&suggested_nickname=${enc(result.suggestedNickname)}"
                }
                redirect(target)
            },
        )
    }

    @PostMapping("/link-provider")
    fun linkProvider(@Valid @RequestBody request: LinkProviderRequest): ResponseEntity<*> {
        return authService.linkProvider(request.linkToken, request.confirm).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { result ->
                when (result) {
                    is LinkProviderResult.LoggedIn ->
                        ResponseEntity.ok(TokenPairResponse(result.tokens.accessToken, result.tokens.refreshToken))
                    is LinkProviderResult.SignupRequired ->
                        ResponseEntity.ok(SignupTokenResponse(result.signupToken, result.suggestedNickname))
                }
            },
        )
    }

    @PostMapping("/oauth/complete-signup")
    fun completeOAuthSignup(@Valid @RequestBody request: CompleteOAuthSignupRequest): ResponseEntity<*> {
        return authService.completeOAuthSignup(request.signupToken, request.nickname).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { pair -> ResponseEntity.ok(TokenPairResponse(pair.accessToken, pair.refreshToken)) },
        )
    }

    // ───────────── Existing endpoints ─────────────

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<*> {
        return authService.refreshToken(request.refreshToken).fold(
            onLeft = { it.toResponseEntity() },
            onRight = { pair -> ResponseEntity.ok(TokenPairResponse(pair.accessToken, pair.refreshToken)) },
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

    // ───────────── Helpers ─────────────

    private fun redirect(url: String): ResponseEntity<Void> {
        val headers = HttpHeaders()
        headers.location = URI.create(url)
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    private fun enc(s: String): String =
        java.net.URLEncoder.encode(s, Charsets.UTF_8)
}
