package com.geekchat.server.domain.model

import java.time.Instant

data class UserProvider(
    val id: String,
    val userId: String,
    val provider: AuthProvider,
    val providerId: String,
    val email: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
