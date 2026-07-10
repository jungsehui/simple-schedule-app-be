package com.geekchat.server.auth.application.port.out

import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either

/**
 * Outbound port for issuing and parsing JWTs used by the auth flows.
 *
 * Extracted so [com.geekchat.server.auth.application.service.AuthService] depends on an
 * application-layer port rather than the infrastructure JwtTokenProvider directly
 * (hexagonal boundary). Implemented in production by the infrastructure JwtTokenProvider.
 */
interface TokenService {
    fun generateAccessToken(userId: String, role: String): String
    fun generateLinkToken(claims: Map<String, Any>, expiryMinutes: Long = 10): String
    fun generateSignupToken(claims: Map<String, Any>, expiryMinutes: Long = 10): String
    fun parseLinkToken(token: String): Either<ChatError, Map<String, Any>>
    fun parseSignupToken(token: String): Either<ChatError, Map<String, Any>>
}
