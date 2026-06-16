package com.geekchat.server.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: JwtProperties,
    val frontendUrl: String = "http://localhost:3000",
    // Additional CORS allow patterns (e.g. https://[wildcard].vercel.app for preview deploys).
    val frontendOriginPatterns: List<String> = emptyList(),
    val oauth: OAuthProperties = OAuthProperties(),
) {
    data class JwtProperties(
        // No default: the app must fail fast at startup if JWT_SECRET is not provided
        // (prod sets it via env; dev/test set it in their profile yml).
        val secret: String,
        val accessTokenExpiry: Duration = Duration.ofMinutes(15),
        val refreshTokenExpiryDays: Long = 14,
    ) {
        init {
            require(secret.toByteArray().size >= 32) {
                "app.jwt.secret must be at least 32 bytes (256-bit) for HS256 — set the JWT_SECRET env var"
            }
        }
    }

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
