package com.geekchat.server.adapter.`in`.web.dto

import com.geekchat.server.domain.model.User
import jakarta.validation.constraints.NotBlank

// --- Requests ---

data class RefreshTokenRequest(
    @field:NotBlank(message = "refreshToken is required")
    val refreshToken: String = "",
)

data class LogoutRequest(
    val refreshToken: String = "",
)

// --- Responses ---

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

data class ErrorResponse(
    val statusCode: Int,
    val message: String,
    val error: String? = null,
)
