package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.OAuthClient
import com.geekchat.server.application.port.out.OAuthProfile
import com.geekchat.server.application.port.out.RefreshTokenRepository
import com.geekchat.server.application.port.out.UserProviderRepository
import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.AuthProvider
import com.geekchat.server.domain.model.RefreshToken
import com.geekchat.server.domain.model.User
import com.geekchat.server.domain.model.UserProvider
import com.geekchat.server.domain.model.UserStatus
import com.geekchat.server.infrastructure.config.AppProperties
import com.geekchat.server.infrastructure.security.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Characterization tests for the OAuth state machine in AuthService.
 * These tests pin the current branching behaviour so that an upgrade
 * (Spring Boot 4, Spring Modulith, etc.) cannot silently change it.
 *
 * Branch map (mirrors AuthService.oauthCallback + handleOAuthCode):
 *  (a)   existing provider + ACTIVE user          → Right(LoggedIn)
 *  (a')  existing provider + WITHDRAWN user       → Left(AccountWithdrawn)
 *  (a'') existing provider + user not found       → Left(UserNotFound)
 *  (b)   no provider match, email match ACTIVE    → Right(LinkingRequired)
 *  (c)   no provider match, no email match        → Right(SignupRequired)
 *  handleOAuthCode short-circuit on exchange error → Left (propagated)
 */
class AuthServiceOAuthTest {

    private val userRepository = mockk<UserRepository>()
    private val userProviderRepository = mockk<UserProviderRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordEncoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
    private val oauthClient = mockk<OAuthClient>()

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
            jwtTokenProvider, appProperties, passwordEncoder, oauthClient,
        )
    }

    // ── (a) existing provider, ACTIVE user → LoggedIn ──────────────────────

    @Test
    fun `oauthCallback existing provider with active user returns LoggedIn with token pair`() {
        val provider = UserProvider(
            id = "p1", userId = "u1", provider = AuthProvider.GOOGLE, providerId = "g100",
        )
        val user = User(id = "u1", nickname = "Bob", status = UserStatus.ACTIVE)
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g100") } returns provider
        every { userRepository.findById("u1") } returns user
        val tokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val profile = OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g100",
            email = "bob@example.com",
            nickname = "Bob",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        val loggedIn = result.getOrNull() as OAuthCallbackResult.LoggedIn
        assertTrue(loggedIn.tokens.accessToken.isNotBlank())
        assertTrue(loggedIn.tokens.refreshToken.isNotBlank())
        verify { refreshTokenRepository.save(any()) }
    }

    // ── (a') existing provider, WITHDRAWN user → AccountWithdrawn ──────────

    @Test
    fun `oauthCallback existing provider with withdrawn user returns AccountWithdrawn`() {
        val provider = UserProvider(
            id = "p2", userId = "u2", provider = AuthProvider.GOOGLE, providerId = "g200",
        )
        val withdrawnUser = User(
            id = "u2", nickname = "deleted_user_xx", status = UserStatus.WITHDRAWN,
        )
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g200") } returns provider
        every { userRepository.findById("u2") } returns withdrawnUser

        val profile = OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g200",
            email = "withdrawn@example.com",
            nickname = "Withdrawn",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.AccountWithdrawn)
    }

    // ── (a'') existing provider, user record missing → UserNotFound ─────────

    @Test
    fun `oauthCallback existing provider but user not found returns UserNotFound`() {
        val provider = UserProvider(
            id = "p3", userId = "u3", provider = AuthProvider.NAVER, providerId = "n300",
        )
        every { userProviderRepository.findByProviderAndProviderId(AuthProvider.NAVER, "n300") } returns provider
        every { userRepository.findById("u3") } returns null

        val profile = OAuthProfile(
            provider = AuthProvider.NAVER,
            providerId = "n300",
            email = "ghost@example.com",
            nickname = "Ghost",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.UserNotFound)
    }

    // ── (b) no provider match, email match ACTIVE → LinkingRequired ─────────

    @Test
    fun `oauthCallback no provider match but email matches active user returns LinkingRequired`() {
        val emailMatchProvider = UserProvider(
            id = "p4", userId = "u4", provider = AuthProvider.GOOGLE, providerId = "g400",
        )
        val existingUser = User(id = "u4", nickname = "Carol", status = UserStatus.ACTIVE)
        every {
            userProviderRepository.findByProviderAndProviderId(AuthProvider.NAVER, "n400")
        } returns null
        every { userProviderRepository.findByEmail("carol@example.com") } returns emailMatchProvider
        every { userRepository.findById("u4") } returns existingUser

        val profile = OAuthProfile(
            provider = AuthProvider.NAVER,
            providerId = "n400",
            email = "carol@example.com",
            nickname = "CarolNaver",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        val linking = result.getOrNull() as OAuthCallbackResult.LinkingRequired
        assertTrue(linking.linkToken.isNotBlank())
        assertEquals("Carol", linking.existingNickname)
        assertEquals(AuthProvider.NAVER, linking.newProvider)
    }

    // ── (c) no provider match, no email match → SignupRequired ──────────────

    @Test
    fun `oauthCallback no provider match and no email match returns SignupRequired`() {
        every {
            userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g500")
        } returns null
        every { userProviderRepository.findByEmail(any()) } returns null

        val profile = OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g500",
            email = "newuser@example.com",
            nickname = "NewUser",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        val signupReq = result.getOrNull() as OAuthCallbackResult.SignupRequired
        assertTrue(signupReq.signupToken.isNotBlank())
        assertEquals("NewUser", signupReq.suggestedNickname)
    }

    // ── (c) null email variant: no email → bypass email lookup → SignupRequired ─

    @Test
    fun `oauthCallback null email skips email lookup and returns SignupRequired`() {
        every {
            userProviderRepository.findByProviderAndProviderId(AuthProvider.NAVER, "n600")
        } returns null

        val profile = OAuthProfile(
            provider = AuthProvider.NAVER,
            providerId = "n600",
            email = null,
            nickname = "NoEmail",
            profileImageUrl = null,
        )
        val result = authService.oauthCallback(profile)

        assertTrue(result.isRight)
        val signupReq = result.getOrNull() as OAuthCallbackResult.SignupRequired
        assertTrue(signupReq.signupToken.isNotBlank())
        assertEquals("NoEmail", signupReq.suggestedNickname)
        // email lookup must not be called when email is null
        verify(exactly = 0) { userProviderRepository.findByEmail(any()) }
    }

    // ── handleOAuthCode: exchange error is propagated, oauthCallback not called ─

    @Test
    fun `handleOAuthCode propagates Left from exchangeCodeForProfile without calling oauthCallback`() {
        every {
            oauthClient.exchangeCodeForProfile(AuthProvider.GOOGLE, "bad-code")
        } returns Either.Left(ChatError.OAuthExchangeFailed("GOOGLE", "exchange failed"))

        val result = authService.handleOAuthCode(AuthProvider.GOOGLE, "bad-code")

        assertTrue(result.isLeft)
        val error = (result as Either.Left).value
        assertTrue(error is ChatError.OAuthExchangeFailed)
        // Repositories must NOT be touched — no exchange, no callback processing
        verify(exactly = 0) { userProviderRepository.findByProviderAndProviderId(any(), any()) }
        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    // ── handleOAuthCode happy path delegates to oauthCallback ───────────────

    @Test
    fun `handleOAuthCode delegates to oauthCallback on successful code exchange`() {
        val profile = OAuthProfile(
            provider = AuthProvider.GOOGLE,
            providerId = "g700",
            email = "delegate@example.com",
            nickname = "Delegate",
            profileImageUrl = null,
        )
        every {
            oauthClient.exchangeCodeForProfile(AuthProvider.GOOGLE, "good-code")
        } returns Either.Right(profile)

        every {
            userProviderRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g700")
        } returns null
        every { userProviderRepository.findByEmail("delegate@example.com") } returns null

        val result = authService.handleOAuthCode(AuthProvider.GOOGLE, "good-code")

        assertTrue(result.isRight)
        assertTrue(result.getOrNull() is OAuthCallbackResult.SignupRequired)
    }
}
