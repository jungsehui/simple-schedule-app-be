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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
            jwtTokenProvider, appProperties,
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
}
