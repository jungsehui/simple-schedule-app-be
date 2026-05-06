package com.geekchat.server.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: JwtProperties = JwtProperties(),
    val frontendUrl: String = "http://localhost:3000",
    // Additional CORS allow patterns (e.g. https://[wildcard].vercel.app for preview deploys).
    val frontendOriginPatterns: List<String> = emptyList(),
    val oauth: OAuthProperties = OAuthProperties(),
) {
    data class JwtProperties(
        val secret: String = "dev-secret-key-must-be-at-least-32-characters-long",
        val accessTokenExpiry: Duration = Duration.ofMinutes(15),
        val refreshTokenExpiryDays: Long = 14,
    )

    data class OAuthProperties(
        val callbackUrl: String = "",
        val google: ProviderProperties = ProviderProperties(),
        val naver: ProviderProperties = ProviderProperties(),
    )

    data class ProviderProperties(
        val clientId: String = "",
        val clientSecret: String = "",
    )
}
