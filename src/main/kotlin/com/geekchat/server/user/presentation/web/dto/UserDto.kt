package com.geekchat.server.user.presentation.web.dto

import com.geekchat.server.user.domain.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class SetUsernameRequest(
    @field:NotBlank(message = "Username is required")
    @field:Pattern(
        regexp = "^[a-z0-9_]{3,20}$",
        message = "Username must be 3-20 characters, lowercase letters, numbers, and underscores only",
    )
    val username: String = "",
)

data class UserSearchResponse(
    val id: String,
    val nickname: String,
    val username: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(user: User): UserSearchResponse = UserSearchResponse(
            id = user.id,
            nickname = user.nickname,
            username = user.username,
            profileImageUrl = user.profileImageUrl,
        )
    }
}
