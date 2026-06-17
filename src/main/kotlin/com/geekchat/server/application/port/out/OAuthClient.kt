package com.geekchat.server.application.port.out

import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.AuthProvider

/**
 * Outbound port for exchanging an OAuth authorization code for the user's profile.
 * Implemented by [com.geekchat.server.adapter.out.oauth.OAuthClientAdapter] in production.
 * Mocked in tests.
 */
interface OAuthClient {
    fun exchangeCodeForProfile(provider: AuthProvider, code: String): Either<ChatError, OAuthProfile>
}

data class OAuthProfile(
    val provider: AuthProvider,
    val providerId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
)
