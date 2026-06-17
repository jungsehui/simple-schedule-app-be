package com.geekchat.server.room.domain.model

import java.security.SecureRandom
import java.time.Instant

data class InviteLink(
    val id: String,
    val code: String,
    val roomId: String,
    val creatorId: String,
    val expiresAt: Instant,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now >= expiresAt
    fun isMaxUsesReached(): Boolean = maxUses != null && currentUses >= maxUses
    fun withIncrementedUses(): InviteLink = copy(currentUses = currentUses + 1, updatedAt = Instant.now())

    companion object {
        private const val CODE_LENGTH = 8
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private val random = SecureRandom()

        fun generateCode(): String = (1..CODE_LENGTH).map { CHARS[random.nextInt(CHARS.length)] }.joinToString("")
    }
}
