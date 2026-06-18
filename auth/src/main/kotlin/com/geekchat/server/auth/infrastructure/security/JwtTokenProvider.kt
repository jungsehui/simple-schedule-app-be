package com.geekchat.server.auth.infrastructure.security

import com.geekchat.server.auth.application.port.out.TokenService
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.common.config.AppProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date

/** Identity extracted from a validated access token: subject + role claim. */
data class AuthenticatedUser(val userId: String, val role: String)

@Component
class JwtTokenProvider(
    private val appProperties: AppProperties,
) : TokenService {
    private val key by lazy {
        Keys.hmacShaKeyFor(appProperties.jwt.secret.toByteArray())
    }

    override fun generateAccessToken(userId: String, role: String): String {
        val now = Date()
        val expiry = Date(now.time + appProperties.jwt.accessTokenExpiry.toMillis())

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * Generic short-lived JWT for OAuth flows (linkToken / signupToken).
     * Subject is left empty; consumers should put context into [claims].
     */
    fun generateClaimsToken(claims: Map<String, Any>, expiryMinutes: Long = 10): String {
        val now = Date()
        val expiry = Date(now.time + expiryMinutes * 60 * 1000)

        val builder = Jwts.builder()
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
        claims.forEach { (k, v) -> builder.claim(k, v) }

        return builder.compact()
    }

    /** Backwards-compatible alias used by older callers. */
    override fun generateLinkToken(claims: Map<String, Any>, expiryMinutes: Long): String =
        generateClaimsToken(claims, expiryMinutes)

    /** Issues a 10-min "signup" token used for OAuth-completion flow (Deferred user creation). */
    override fun generateSignupToken(claims: Map<String, Any>, expiryMinutes: Long): String =
        generateClaimsToken(claims, expiryMinutes)

    fun validateToken(token: String): Either<ChatError, AuthenticatedUser> {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            val role = claims["role"] as? String ?: "USER" // tokens issued before role claim → USER
            Either.Right(AuthenticatedUser(userId = claims.subject, role = role))
        } catch (e: ExpiredJwtException) {
            Either.Left(ChatError.TokenExpired())
        } catch (_: Exception) {
            Either.Left(ChatError.InvalidToken())
        }
    }

    override fun parseLinkToken(token: String): Either<ChatError, Map<String, Any>> {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            Either.Right(claims.toMap())
        } catch (_: ExpiredJwtException) {
            Either.Left(ChatError.InvalidLinkToken())
        } catch (_: Exception) {
            Either.Left(ChatError.InvalidLinkToken())
        }
    }

    override fun parseSignupToken(token: String): Either<ChatError, Map<String, Any>> {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            Either.Right(claims.toMap())
        } catch (_: ExpiredJwtException) {
            Either.Left(ChatError.InvalidSignupToken())
        } catch (_: Exception) {
            Either.Left(ChatError.InvalidSignupToken())
        }
    }
}
