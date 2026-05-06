package com.geekchat.server.adapter.`in`.web.dto

import com.geekchat.server.domain.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// ───── Requests ─────

data class RefreshTokenRequest(
    @field:NotBlank(message = "refreshToken is required")
    val refreshToken: String = "",
)

data class LogoutRequest(
    val refreshToken: String = "",
)

data class SignupRequest(
    @field:NotBlank
    @field:Pattern(
        regexp = "^[a-z0-9_]{3,20}$",
        message = "Username must be 3-20 chars, lowercase letters/digits/underscore",
    )
    val username: String = "",

    @field:NotBlank
    @field:Size(min = 8, max = 72, message = "Password must be 8-72 chars")
    val password: String = "",

    @field:NotBlank
    @field:Size(min = 1, max = 20, message = "Nickname must be 1-20 chars")
    val nickname: String = "",

    val email: String? = null,
)

data class LoginRequest(
    @field:NotBlank
    val username: String = "",
    @field:NotBlank
    val password: String = "",
)

data class LinkProviderRequest(
    @field:NotBlank(message = "linkToken is required")
    val linkToken: String = "",
    val confirm: Boolean = false,
)

data class CompleteOAuthSignupRequest(
    @field:NotBlank(message = "signupToken is required")
    val signupToken: String = "",
    @field:NotBlank
    @field:Size(min = 1, max = 20)
    val nickname: String = "",
)

// ───── Responses ─────

data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class DevLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val message: String,
)

data class UserMeResponse(
    val id: String,
    val nickname: String,
    val username: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(user: User): UserMeResponse = UserMeResponse(
            id = user.id,
            nickname = user.nickname,
            username = user.username,
            profileImageUrl = user.profileImageUrl,
        )
    }
}

data class SignupTokenResponse(
    val signupToken: String,
    val suggestedNickname: String,
)

data class ErrorResponse(
    val statusCode: Int,
    val message: String,
    val error: String? = null,
)
