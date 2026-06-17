package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.OAuthClient
import com.geekchat.server.application.port.out.OAuthProfile
import com.geekchat.server.application.port.out.RefreshTokenRepository
import com.geekchat.server.user.domain.repository.UserProviderRepository
import com.geekchat.server.user.domain.repository.UserRepository
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.user.domain.model.AuthProvider
import com.geekchat.server.domain.model.RefreshToken
import com.geekchat.server.user.domain.model.User
import com.geekchat.server.user.domain.model.UserProvider
import com.geekchat.server.user.domain.model.UserStatus
import com.geekchat.server.infrastructure.config.AppProperties
import com.geekchat.server.infrastructure.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

data class DevLoginResult(
    val accessToken: String,
    val refreshToken: String,
    val message: String,
)

data class SignupCommand(
    val username: String,
    val password: String,
    val nickname: String,
    val email: String? = null,
)

/**
 * Result of an OAuth provider callback.
 *
 * - [LoggedIn]: existing UserProvider matched → tokens issued.
 * - [LinkingRequired]: same email exists with a different provider → user must decide.
 * - [SignupRequired]: brand-new social identity → user must pick a nickname.
 */
sealed class OAuthCallbackResult {
    data class LoggedIn(val tokens: TokenPair) : OAuthCallbackResult()
    data class LinkingRequired(
        val linkToken: String,
        val existingNickname: String,
        val newProvider: AuthProvider,
    ) : OAuthCallbackResult()
    data class SignupRequired(
        val signupToken: String,
        val suggestedNickname: String,
    ) : OAuthCallbackResult()
}

sealed class LinkProviderResult {
    data class LoggedIn(val tokens: TokenPair) : LinkProviderResult()
    data class SignupRequired(
        val signupToken: String,
        val suggestedNickname: String,
    ) : LinkProviderResult()
}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userProviderRepository: UserProviderRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val appProperties: AppProperties,
    private val passwordEncoder: PasswordEncoder,
    private val oauthClient: OAuthClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ────────────── Local (ID/PW) ──────────────

    @Transactional
    fun signup(cmd: SignupCommand): Either<ChatError, TokenPair> {
        if (!User.USERNAME_PATTERN.matches(cmd.username)) {
            return Either.Left(ChatError.InvalidUsername())
        }
        validatePassword(cmd.password)?.let { return Either.Left(it) }
        if (cmd.nickname.isBlank() || cmd.nickname.length > User.MAX_NICKNAME_LENGTH) {
            return Either.Left(ChatError.NicknameRequired())
        }
        if (userRepository.existsByUsername(cmd.username)) {
            return Either.Left(ChatError.UsernameAlreadyTaken(cmd.username))
        }
        if (!cmd.email.isNullOrBlank() && userRepository.existsByEmailAndStatusActive(cmd.email)) {
            return Either.Left(ChatError.EmailAlreadyInUse(cmd.email))
        }

        val saved = userRepository.save(
            User(
                id = UUID.randomUUID().toString(),
                nickname = cmd.nickname,
                username = cmd.username,
                email = cmd.email?.takeIf { it.isNotBlank() },
                passwordHash = passwordEncoder.encode(cmd.password),
                status = UserStatus.ACTIVE,
            ),
        )
        log.info("user_signup userId={} username={}", saved.id, saved.username)
        return Either.Right(issueTokenPair(saved.id))
    }

    @Transactional
    fun login(username: String, password: String): Either<ChatError, TokenPair> {
        val user = userRepository.findByUsername(username)
            ?: return Either.Left(ChatError.InvalidCredentials())

        if (user.status == UserStatus.WITHDRAWN) {
            return Either.Left(ChatError.AccountWithdrawn())
        }
        val hash = user.passwordHash
            ?: return Either.Left(ChatError.InvalidCredentials())
        if (!passwordEncoder.matches(password, hash)) {
            return Either.Left(ChatError.InvalidCredentials())
        }

        log.info("user_login userId={}", user.id)
        return Either.Right(issueTokenPair(user.id))
    }

    @Transactional
    fun withdraw(userId: String): Either<ChatError, Unit> {
        val user = userRepository.findById(userId)
            ?: return Either.Left(ChatError.UserNotFound(userId))

        if (user.status == UserStatus.WITHDRAWN) {
            return Either.Right(Unit) // idempotent
        }

        userRepository.save(user.anonymize())
        userProviderRepository.deleteAllByUserId(userId)
        refreshTokenRepository.deleteAllByUserId(userId)

        log.info("user_withdraw userId={}", userId)
        return Either.Right(Unit)
    }

    // ────────────── OAuth ──────────────

    @Transactional
    fun handleOAuthCode(provider: AuthProvider, code: String): Either<ChatError, OAuthCallbackResult> {
        val profileResult = oauthClient.exchangeCodeForProfile(provider, code)
        val profile = when (profileResult) {
            is Either.Left -> return Either.Left(profileResult.value)
            is Either.Right -> profileResult.value
        }
        return oauthCallback(profile)
    }

    @Transactional
    fun oauthCallback(profile: OAuthProfile): Either<ChatError, OAuthCallbackResult> {
        // (a) existing provider link → log in
        val existingProvider = userProviderRepository.findByProviderAndProviderId(profile.provider, profile.providerId)
        if (existingProvider != null) {
            val user = userRepository.findById(existingProvider.userId)
                ?: return Either.Left(ChatError.UserNotFound(existingProvider.userId))
            if (user.status == UserStatus.WITHDRAWN) {
                return Either.Left(ChatError.AccountWithdrawn())
            }
            return Either.Right(OAuthCallbackResult.LoggedIn(issueTokenPair(user.id)))
        }

        // (b) email matches another provider → linking required
        val email = profile.email
        if (!email.isNullOrBlank()) {
            val emailMatch = userProviderRepository.findByEmail(email)
            if (emailMatch != null) {
                val existingUser = userRepository.findById(emailMatch.userId)
                if (existingUser != null && existingUser.status == UserStatus.ACTIVE) {
                    val linkToken = jwtTokenProvider.generateLinkToken(
                        mapOf(
                            "sub" to existingUser.id,
                            "linkProvider" to profile.provider.name,
                            "linkProviderId" to profile.providerId,
                            "linkEmail" to (profile.email ?: ""),
                            "linkNickname" to profile.nickname,
                            "linkProfileImageUrl" to (profile.profileImageUrl ?: ""),
                        ),
                    )
                    return Either.Right(
                        OAuthCallbackResult.LinkingRequired(
                            linkToken = linkToken,
                            existingNickname = existingUser.nickname,
                            newProvider = profile.provider,
                        ),
                    )
                }
            }
        }

        // (c) brand new social identity → signup required (deferred user creation)
        val signupToken = jwtTokenProvider.generateSignupToken(
            mapOf(
                "signupProvider" to profile.provider.name,
                "signupProviderId" to profile.providerId,
                "signupEmail" to (profile.email ?: ""),
                "signupNickname" to profile.nickname,
                "signupProfileImageUrl" to (profile.profileImageUrl ?: ""),
            ),
        )
        return Either.Right(
            OAuthCallbackResult.SignupRequired(
                signupToken = signupToken,
                suggestedNickname = profile.nickname,
            ),
        )
    }

    @Transactional
    fun linkProvider(linkToken: String, confirm: Boolean): Either<ChatError, LinkProviderResult> {
        val claims = jwtTokenProvider.parseLinkToken(linkToken)
        val payload = when (claims) {
            is Either.Left -> return Either.Left(claims.value)
            is Either.Right -> claims.value
        }
        val existingUserId = payload["sub"] as? String ?: return Either.Left(ChatError.InvalidLinkToken())
        val providerName = payload["linkProvider"] as? String ?: return Either.Left(ChatError.InvalidLinkToken())
        val providerId = payload["linkProviderId"] as? String ?: return Either.Left(ChatError.InvalidLinkToken())
        val email = payload["linkEmail"] as? String
        val nickname = payload["linkNickname"] as? String ?: "user"
        val profileImageUrl = payload["linkProfileImageUrl"] as? String

        val provider = runCatching { AuthProvider.valueOf(providerName) }.getOrNull()
            ?: return Either.Left(ChatError.InvalidLinkToken())

        if (!confirm) {
            // user said "no, create new account" → fall through to signup-required
            val signupToken = jwtTokenProvider.generateSignupToken(
                mapOf(
                    "signupProvider" to providerName,
                    "signupProviderId" to providerId,
                    "signupEmail" to (email ?: ""),
                    "signupNickname" to nickname,
                    "signupProfileImageUrl" to (profileImageUrl ?: ""),
                ),
            )
            return Either.Right(
                LinkProviderResult.SignupRequired(
                    signupToken = signupToken,
                    suggestedNickname = nickname,
                ),
            )
        }

        // confirm=true: link new provider to existing user
        val user = userRepository.findById(existingUserId)
            ?: return Either.Left(ChatError.UserNotFound(existingUserId))
        if (user.status == UserStatus.WITHDRAWN) {
            return Either.Left(ChatError.AccountWithdrawn())
        }

        val already = userProviderRepository.findByProviderAndProviderId(provider, providerId)
        if (already != null) {
            return Either.Left(ChatError.ProviderAlreadyLinked())
        }

        userProviderRepository.save(
            UserProvider(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                provider = provider,
                providerId = providerId,
                email = email?.takeIf { it.isNotBlank() },
            ),
        )
        log.info("provider_linked userId={} provider={}", user.id, provider)
        return Either.Right(LinkProviderResult.LoggedIn(issueTokenPair(user.id)))
    }

    @Transactional
    fun completeOAuthSignup(signupToken: String, nickname: String): Either<ChatError, TokenPair> {
        if (nickname.isBlank() || nickname.length > User.MAX_NICKNAME_LENGTH) {
            return Either.Left(ChatError.NicknameRequired())
        }
        val claims = jwtTokenProvider.parseSignupToken(signupToken)
        val payload = when (claims) {
            is Either.Left -> return Either.Left(claims.value)
            is Either.Right -> claims.value
        }
        val providerName = payload["signupProvider"] as? String ?: return Either.Left(ChatError.InvalidSignupToken())
        val providerId = payload["signupProviderId"] as? String ?: return Either.Left(ChatError.InvalidSignupToken())
        val email = payload["signupEmail"] as? String
        val profileImageUrl = payload["signupProfileImageUrl"] as? String

        val provider = runCatching { AuthProvider.valueOf(providerName) }.getOrNull()
            ?: return Either.Left(ChatError.InvalidSignupToken())

        // Defensive: if a UserProvider was created in the meantime (replay), log them in.
        val already = userProviderRepository.findByProviderAndProviderId(provider, providerId)
        if (already != null) {
            val existing = userRepository.findById(already.userId)
                ?: return Either.Left(ChatError.UserNotFound(already.userId))
            return Either.Right(issueTokenPair(existing.id))
        }

        val newUser = userRepository.save(
            User(
                id = UUID.randomUUID().toString(),
                nickname = nickname.trim(),
                email = email?.takeIf { it.isNotBlank() },
                profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
                status = UserStatus.ACTIVE,
            ),
        )
        userProviderRepository.save(
            UserProvider(
                id = UUID.randomUUID().toString(),
                userId = newUser.id,
                provider = provider,
                providerId = providerId,
                email = email?.takeIf { it.isNotBlank() },
            ),
        )
        log.info("oauth_signup_completed userId={} provider={}", newUser.id, provider)
        return Either.Right(issueTokenPair(newUser.id))
    }

    // ────────────── Existing methods (unchanged) ──────────────

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

        if (user.status == UserStatus.WITHDRAWN) {
            return Either.Left(ChatError.AccountWithdrawn())
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

        // Block refresh for withdrawn accounts (defensive: refresh tokens are deleted on withdraw)
        val user = userRepository.findById(storedToken.userId)
        if (user != null && user.status == UserStatus.WITHDRAWN) {
            refreshTokenRepository.deleteByToken(token)
            return Either.Left(ChatError.AccountWithdrawn())
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

    // ────────────── Private helpers ──────────────

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

    private fun validatePassword(password: String): ChatError? {
        if (password.length < 8) return ChatError.WeakPassword(reason = "Password must be at least 8 characters")
        if (!password.any { it.isDigit() }) return ChatError.WeakPassword(reason = "Password must contain at least one digit")
        if (!password.any { it.isLetter() }) return ChatError.WeakPassword(reason = "Password must contain at least one letter")
        return null
    }
}
