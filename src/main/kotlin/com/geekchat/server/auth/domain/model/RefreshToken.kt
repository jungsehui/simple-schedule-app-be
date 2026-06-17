package com.geekchat.server.auth.domain.model

import java.time.Instant

data class RefreshToken(
    val id: String,
    val userId: String,
    val token: String,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
