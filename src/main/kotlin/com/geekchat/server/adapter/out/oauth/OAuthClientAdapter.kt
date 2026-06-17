package com.geekchat.server.adapter.out.oauth

import tools.jackson.databind.JsonNode
import com.geekchat.server.application.port.out.OAuthClient
import com.geekchat.server.application.port.out.OAuthProfile
import com.geekchat.server.domain.error.ChatError
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.model.AuthProvider
import com.geekchat.server.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Production OAuth client. Exchanges authorization codes for user profiles via
 * Google's and Naver's OAuth2/OpenID Connect endpoints.
 *
 * Endpoints (from v1 reference auth.controller.ts:259-317):
 * - Google: token=https://oauth2.googleapis.com/token, profile=https://www.googleapis.com/oauth2/v2/userinfo
 * - Naver:  token=https://nid.naver.com/oauth2.0/token, profile=https://openapi.naver.com/v1/nid/me
 */
@Component
class OAuthClientAdapter(
    private val appProperties: AppProperties,
) : OAuthClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = RestClient.create()

    override fun exchangeCodeForProfile(provider: AuthProvider, code: String): Either<ChatError, OAuthProfile> {
        return try {
            when (provider) {
                AuthProvider.GOOGLE -> exchangeGoogle(code)
                AuthProvider.NAVER -> exchangeNaver(code)
            }
        } catch (e: RestClientException) {
            log.warn("oauth_exchange_http_error provider={} reason={}", provider, e.message)
            Either.Left(ChatError.OAuthExchangeFailed(provider.name, e.message ?: "HTTP error"))
        } catch (e: Exception) {
            log.error("oauth_exchange_unexpected_error provider={} reason={}", provider, e.message, e)
            Either.Left(ChatError.OAuthExchangeFailed(provider.name, e.message ?: "Unexpected error"))
        }
    }

    private fun exchangeGoogle(code: String): Either<ChatError, OAuthProfile> {
        val cfg = appProperties.oauth.google
        if (cfg.clientId.isBlank() || cfg.clientSecret.isBlank()) {
            return Either.Left(ChatError.OAuthExchangeFailed("GOOGLE", "Google OAuth not configured"))
        }

        val tokenForm = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", cfg.clientId)
            add("client_secret", cfg.clientSecret)
            add("redirect_uri", appProperties.oauth.callbackUrl)
            add("grant_type", "authorization_code")
        }

        val tokenResp = rest.post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(tokenForm)
            .retrieve()
            .body(JsonNode::class.java)
            ?: return Either.Left(ChatError.OAuthExchangeFailed("GOOGLE", "Empty token response"))

        val accessToken = tokenResp.get("access_token")?.asText()
            ?: return Either.Left(ChatError.OAuthExchangeFailed("GOOGLE", "Missing access_token"))

        val profile = rest.get()
            .uri("https://www.googleapis.com/oauth2/v2/userinfo")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(JsonNode::class.java)
            ?: return Either.Left(ChatError.OAuthExchangeFailed("GOOGLE", "Empty profile response"))

        val providerId = profile.get("id")?.asText()
            ?: return Either.Left(ChatError.OAuthProfileMissingId("GOOGLE"))

        return Either.Right(
            OAuthProfile(
                provider = AuthProvider.GOOGLE,
                providerId = providerId,
                email = profile.get("email")?.asText(),
                nickname = profile.get("name")?.asText() ?: "user_${providerId.take(6)}",
                profileImageUrl = profile.get("picture")?.asText(),
            ),
        )
    }

    private fun exchangeNaver(code: String): Either<ChatError, OAuthProfile> {
        val cfg = appProperties.oauth.naver
        if (cfg.clientId.isBlank() || cfg.clientSecret.isBlank()) {
            return Either.Left(ChatError.OAuthExchangeFailed("NAVER", "Naver OAuth not configured"))
        }

        val tokenForm = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", cfg.clientId)
            add("client_secret", cfg.clientSecret)
            add("code", code)
        }

        val tokenResp = rest.post()
            .uri("https://nid.naver.com/oauth2.0/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(tokenForm)
            .retrieve()
            .body(JsonNode::class.java)
            ?: return Either.Left(ChatError.OAuthExchangeFailed("NAVER", "Empty token response"))

        val accessToken = tokenResp.get("access_token")?.asText()
            ?: return Either.Left(ChatError.OAuthExchangeFailed("NAVER", "Missing access_token"))

        val profile = rest.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(JsonNode::class.java)
            ?: return Either.Left(ChatError.OAuthExchangeFailed("NAVER", "Empty profile response"))

        val response = profile.get("response")
            ?: return Either.Left(ChatError.OAuthExchangeFailed("NAVER", "Missing response field"))

        val providerId = response.get("id")?.asText()
            ?: return Either.Left(ChatError.OAuthProfileMissingId("NAVER"))

        return Either.Right(
            OAuthProfile(
                provider = AuthProvider.NAVER,
                providerId = providerId,
                email = response.get("email")?.asText(),
                nickname = response.get("nickname")?.asText()
                    ?: response.get("name")?.asText()
                    ?: "user_${providerId.take(6)}",
                profileImageUrl = response.get("profile_image")?.asText(),
            ),
        )
    }
}
