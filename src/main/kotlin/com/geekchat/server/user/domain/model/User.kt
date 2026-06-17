package com.geekchat.server.user.domain.model

import java.time.Instant

data class User(
    val id: String,
    val nickname: String,
    val username: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val passwordHash: String? = null,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
) {
    companion object {
        const val MAX_NICKNAME_LENGTH = 20
        val USERNAME_PATTERN = Regex("^[a-z0-9_]{3,20}$")
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

    fun withPassword(hash: String): User =
        copy(passwordHash = hash, updatedAt = Instant.now())

    fun hasPassword(): Boolean = passwordHash != null

    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * Anonymizes the user for withdrawal:
     * - Clears identifying fields (email, profileImageUrl, passwordHash)
     * - Replaces nickname with a deterministic placeholder
     * - Sets status to WITHDRAWN (login blocked)
     *
     * Messages and rooms are preserved (handled by service layer).
     */
    fun anonymize(): User {
        val placeholder = "deleted_user_${id.replace("-", "").take(8)}"
        return copy(
            nickname = placeholder,
            username = null,
            email = null,
            profileImageUrl = null,
            passwordHash = null,
            status = UserStatus.WITHDRAWN,
            updatedAt = Instant.now(),
        )
    }
}
