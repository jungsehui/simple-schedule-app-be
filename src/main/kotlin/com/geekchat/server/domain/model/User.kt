package com.geekchat.server.domain.model

import java.time.Instant

data class User(
    val id: String,
    val nickname: String,
    val username: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
) {
    companion object {
        private const val MAX_NICKNAME_LENGTH = 20
        private val USERNAME_PATTERN = Regex("^[a-z0-9_]{3,20}$")
    }

    fun withUsername(username: String): User {
        require(USERNAME_PATTERN.matches(username)) {
            "Username must be 3-20 characters, lowercase letters, numbers, and underscores only"
        }
        return copy(username = username, updatedAt = Instant.now())
    }

    fun withUpdatedProfile(nickname: String): User {
        require(nickname.isNotBlank() && nickname.length <= MAX_NICKNAME_LENGTH) {
            "Nickname must be 1-$MAX_NICKNAME_LENGTH characters"
        }
        return copy(nickname = nickname, updatedAt = Instant.now())
    }
}
