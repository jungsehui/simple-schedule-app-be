package com.geekchat.server.application.service

import com.geekchat.server.auth.application.port.out.OAuthProfile
import com.geekchat.server.auth.application.service.AuthService
import com.geekchat.server.auth.application.service.OAuthCallbackResult
import com.geekchat.server.auth.application.service.SignupCommand
import com.geekchat.server.auth.domain.repository.RefreshTokenRepository
import com.geekchat.server.user.domain.repository.UserProviderRepository
import com.geekchat.server.user.domain.repository.UserRepository
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.user.domain.model.AuthProvider
import com.geekchat.server.auth.domain.model.RefreshToken
import com.geekchat.server.user.domain.model.User
import com.geekchat.server.user.domain.model.UserProvider
import com.geekchat.server.infrastructure.config.AppProperties
import com.geekchat.server.auth.infrastructure.security.JwtTokenProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userProviderRepository = mockk<UserProviderRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordEncoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
    private val oauthClient = mockk<com.geekchat.server.auth.application.port.out.OAuthClient>()

    private val appProperties = AppProperties(
        jwt = AppProperties.JwtProperties(
            secret = "test-secret-key-must-be-at-least-32-characters-long-for-testing",
            accessTokenExpiry = Duration.ofMinutes(15),
            refreshTokenExpiryDays = 14,
        ),
    )
    private val jwtTokenProvider = JwtTokenProvider(appProperties)

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository, userProviderRepository, refreshTokenRepository,
            jwtTokenProvider, appProperties.jwt.refreshTokenExpiryDays, passwordEncoder, oauthClient,
        )
    }

    @Test
    fun `devLogin creates new user when provider not found`() {
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "dev-Alice") } returns null
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        val providerSlot = slot<UserProvider>()
        every { userProviderRepository.save(capture(providerSlot)) } answers { providerSlot.captured }
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val result = authService.devLogin("Alice")

        assertTrue(result.isRight)
        val login = result.getOrNull()!!
        assertEquals("Logged in as Alice", login.message)
        assertTrue(login.accessToken.isNotBlank())
        assertTrue(login.refreshToken.isNotBlank())
    }

    @Test
    fun `devLogin returns existing user when provider found`() {
        val existingUser = User(id = "u1", nickname = "Alice")
        val provider = UserProvider(id = "p1", userId = "u1", provider = AuthProvider.GOOGLE, providerId = "dev-Alice")
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "dev-Alice") } returns provider
        every { userRepository.findById("u1") } returns existingUser
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val result = authService.devLogin("Alice")

        assertTrue(result.isRight)
        assertEquals("Logged in as Alice", result.getOrNull()!!.message)
    }

    @Test
    fun `refreshToken succeeds with valid token`() {
        val stored = RefreshToken(
            id = "rt1", userId = "u1", token = "valid-token",
            expiresAt = Instant.now().plus(14, ChronoUnit.DAYS),
        )
        every { refreshTokenRepository.findByToken("valid-token") } returns stored
        every { refreshTokenRepository.deleteByToken("valid-token") } just runs
        every { userRepository.findById("u1") } returns User(id = "u1", nickname = "Alice")
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val result = authService.refreshToken("valid-token")

        assertTrue(result.isRight)
        val pair = result.getOrNull()!!
        assertTrue(pair.accessToken.isNotBlank())
        assertTrue(pair.refreshToken.isNotBlank())
    }

    @Test
    fun `refreshToken fails with expired token`() {
        val expired = RefreshToken(
            id = "rt1", userId = "u1", token = "expired-token",
            expiresAt = Instant.now().minus(1, ChronoUnit.DAYS),
        )
        every { refreshTokenRepository.findByToken("expired-token") } returns expired
        every { refreshTokenRepository.deleteByToken("expired-token") } just runs

        val result = authService.refreshToken("expired-token")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.RefreshTokenExpired)
    }

    @Test
    fun `refreshToken fails with unknown token`() {
        every { refreshTokenRepository.findByToken("unknown") } returns null

        val result = authService.refreshToken("unknown")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InvalidRefreshToken)
    }

    @Test
    fun `getCurrentUser succeeds when user exists`() {
        val user = User(id = "u1", nickname = "Alice")
        every { userRepository.findById("u1") } returns user

        val result = authService.getCurrentUser("u1")

        assertTrue(result.isRight)
        assertEquals("Alice", result.getOrNull()!!.nickname)
    }

    @Test
    fun `getCurrentUser fails when user not found`() {
        every { userRepository.findById("missing") } returns null

        val result = authService.getCurrentUser("missing")

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.UserNotFound)
    }

    // ───────── Local signup/login/withdraw ─────────

    @Test
    fun `signup creates user with hashed password`() {
        every { userRepository.existsByUsername("alice") } returns false
        every { userRepository.existsByEmailAndStatusActive(any()) } returns false
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val cmd = com.geekchat.server.auth.application.service.SignupCommand(
            username = "alice", password = "hunter2x", nickname = "Alice",
        )
        val result = authService.signup(cmd)

        assertTrue(result.isRight)
        assertTrue(userSlot.captured.passwordHash != null)
        // BCrypt hash, never plaintext
        assertTrue(userSlot.captured.passwordHash != "hunter2x")
    }

    @Test
    fun `signup rejects weak password`() {
        val cmd = com.geekchat.server.auth.application.service.SignupCommand(
            username = "alice", password = "short1", nickname = "Alice",
        )
        val result = authService.signup(cmd)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.WeakPassword)
    }

    @Test
    fun `signup rejects taken username`() {
        every { userRepository.existsByUsername("alice") } returns true

        val cmd = com.geekchat.server.auth.application.service.SignupCommand(
            username = "alice", password = "hunter2x", nickname = "Alice",
        )
        val result = authService.signup(cmd)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.UsernameAlreadyTaken)
    }

    @Test
    fun `login succeeds with correct password`() {
        val hashed = passwordEncoder.encode("hunter2x")
        val user = User(id = "u1", nickname = "Alice", username = "alice", passwordHash = hashed)
        every { userRepository.findByUsername("alice") } returns user
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val result = authService.login("alice", "hunter2x")
        assertTrue(result.isRight)
    }

    @Test
    fun `login fails with wrong password`() {
        val hashed = passwordEncoder.encode("hunter2x")
        val user = User(id = "u1", nickname = "Alice", username = "alice", passwordHash = hashed)
        every { userRepository.findByUsername("alice") } returns user

        val result = authService.login("alice", "wrong-password")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InvalidCredentials)
    }

    @Test
    fun `login fails when user is withdrawn`() {
        val user = User(
            id = "u1", nickname = "deleted_user_xx", username = null,
            passwordHash = passwordEncoder.encode("hunter2x"),
            status = com.geekchat.server.user.domain.model.UserStatus.WITHDRAWN,
        )
        every { userRepository.findByUsername("alice") } returns user

        val result = authService.login("alice", "hunter2x")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.AccountWithdrawn)
    }

    @Test
    fun `login fails when user has no password`() {
        // OAuth-only user has no passwordHash
        val user = User(id = "u1", nickname = "Alice", username = "alice")
        every { userRepository.findByUsername("alice") } returns user

        val result = authService.login("alice", "hunter2x")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InvalidCredentials)
    }

    @Test
    fun `withdraw anonymizes user, deletes providers and refresh tokens`() {
        val user = User(id = "u1", nickname = "Alice", username = "alice", email = "alice@x.com")
        every { userRepository.findById("u1") } returns user
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        every { userProviderRepository.deleteAllByUserId("u1") } just runs
        every { refreshTokenRepository.deleteAllByUserId("u1") } just runs

        val result = authService.withdraw("u1")

        assertTrue(result.isRight)
        assertEquals(com.geekchat.server.user.domain.model.UserStatus.WITHDRAWN, userSlot.captured.status)
        assertTrue(userSlot.captured.nickname.startsWith("deleted_user_"))
        assertEquals(null, userSlot.captured.email)
        verify { userProviderRepository.deleteAllByUserId("u1") }
        verify { refreshTokenRepository.deleteAllByUserId("u1") }
    }

    @Test
    fun `withdraw is idempotent for already-withdrawn user`() {
        val user = User(
            id = "u1", nickname = "deleted_user_xx",
            status = com.geekchat.server.user.domain.model.UserStatus.WITHDRAWN,
        )
        every { userRepository.findById("u1") } returns user

        val result = authService.withdraw("u1")
        assertTrue(result.isRight)
    }

    // ───────── OAuth ─────────

    @Test
    fun `oauthCallback existing provider returns LoggedIn`() {
        val provider = com.geekchat.server.user.domain.model.UserProvider(
            id = "p1", userId = "u1", provider = AuthProvider.GOOGLE, providerId = "g123",
        )
        val user = User(id = "u1", nickname = "Alice")
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g123") } returns provider
        every { userRepository.findById("u1") } returns user
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val profile = com.geekchat.server.auth.application.port.out.OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g123",
            email = "alice@x.com",
            nickname = "Alice",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        assertTrue(result.getOrNull() is com.geekchat.server.auth.application.service.OAuthCallbackResult.LoggedIn)
    }

    @Test
    fun `oauthCallback no match returns SignupRequired`() {
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g999") } returns null
        every { userProviderRepository.findByEmail(any()) } returns null

        val profile = com.geekchat.server.auth.application.port.out.OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g999",
            email = "newbie@x.com",
            nickname = "Newbie",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        val signupReq = result.getOrNull() as com.geekchat.server.auth.application.service.OAuthCallbackResult.SignupRequired
        assertTrue(signupReq.signupToken.isNotBlank())
        assertEquals("Newbie", signupReq.suggestedNickname)
    }

    @Test
    fun `completeOAuthSignup creates user and issues tokens`() {
        // Generate a valid signup token first via the helper
        val signupToken = jwtTokenProvider.generateSignupToken(
            mapOf(
                "signupProvider" to "GOOGLE",
                "signupProviderId" to "g999",
                "signupEmail" to "newbie@x.com",
                "signupNickname" to "Newbie",
                "signupProfileImageUrl" to "",
            ),
        )
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g999") } returns null
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        every { userProviderRepository.save(any()) } answers { firstArg() }
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        val result = authService.completeOAuthSignup(signupToken, "ChosenName")
        assertTrue(result.isRight)
        assertEquals("ChosenName", userSlot.captured.nickname)
        assertEquals("newbie@x.com", userSlot.captured.email)
    }

    @Test
    fun `completeOAuthSignup rejects empty nickname`() {
        val signupToken = jwtTokenProvider.generateSignupToken(
            mapOf(
                "signupProvider" to "GOOGLE",
                "signupProviderId" to "g999",
            ),
        )

        val result = authService.completeOAuthSignup(signupToken, "")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.NicknameRequired)
    }

    @Test
    fun `completeOAuthSignup with invalid token returns InvalidSignupToken`() {
        val result = authService.completeOAuthSignup("not-a-token", "Newbie")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.InvalidSignupToken)
    }
}
