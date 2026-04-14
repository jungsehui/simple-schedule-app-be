package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.RefreshTokenRepository
import com.geekchat.server.application.port.out.UserProviderRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.domain.error.ChatError
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.model.AuthProvider
import com.geekchat.server.domain.model.RefreshToken
import com.geekchat.server.domain.model.User
import com.geekchat.server.domain.model.UserProvider
import com.geekchat.server.infrastructure.config.AppProperties
import com.geekchat.server.infrastructure.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userProviderRepository: UserProviderRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val appProperties: AppProperties,
) {
    @Transactional
    fun devLogin(name: String): Either<ChatError, DevLoginResult> {
        val providerId = "dev-$name"

        val existingProvider = userProviderRepository.findByProviderAndProviderId(
            AuthProvider.GOOGLE, providerId,
        )

        val user = if (existingProvider != null) {
            userRepository.findById(existingProvider.userId)
                ?: return Either.Left(ChatError.UserNotFound(existingProvider.userId))
        } else {
            val newUser = userRepository.save(
                User(
                    id = UUID.randomUUID().toString(),
                    nickname = name,
                ),
            )
            userProviderRepository.save(
                UserProvider(
                    id = UUID.randomUUID().toString(),
                    userId = newUser.id,
                    provider = AuthProvider.GOOGLE,
                    providerId = providerId,
                ),
            )
            newUser
        }

        val tokenPair = issueTokenPair(user.id)

        return Either.Right(
            DevLoginResult(
                accessToken = tokenPair.accessToken,
                refreshToken = tokenPair.refreshToken,
                message = "Logged in as $name",
            ),
        )
    }

    @Transactional
    fun refreshToken(token: String): Either<ChatError, TokenPair> {
        if (token.isBlank()) {
            return Either.Left(ChatError.RefreshTokenRequired())
        }

        val storedToken = refreshTokenRepository.findByToken(token)
            ?: return Either.Left(ChatError.InvalidRefreshToken())

        if (storedToken.isExpired()) {
            refreshTokenRepository.deleteByToken(token)
            return Either.Left(ChatError.RefreshTokenExpired())
        }

        refreshTokenRepository.deleteByToken(token)

        return Either.Right(issueTokenPair(storedToken.userId))
    }

    fun getCurrentUser(userId: String): Either<ChatError, User> {
        val user = userRepository.findById(userId)
            ?: return Either.Left(ChatError.UserNotFound(userId))
        return Either.Right(user)
    }

    @Transactional
    fun logout(token: String): Either<ChatError, Unit> {
        if (token.isNotBlank()) {
            refreshTokenRepository.deleteByToken(token)
        }
        return Either.Right(Unit)
    }

    private fun issueTokenPair(userId: String): TokenPair {
        val accessToken = jwtTokenProvider.generateAccessToken(userId)
        val refreshTokenValue = UUID.randomUUID().toString()

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                userId = userId,
                token = refreshTokenValue,
                expiresAt = Instant.now().plus(appProperties.jwt.refreshTokenExpiryDays, ChronoUnit.DAYS),
            ),
        )

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshTokenValue,
        )
    }
}

data class DevLoginResult(
    val accessToken: String,
    val refreshToken: String,
    val message: String,
)
