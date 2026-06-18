package com.geekchat.server.common.presentation.web

/**
 * Standard REST error body: { statusCode, message, error }.
 * Shared across all feature modules' controllers via [ChatError.toResponseEntity].
 */
data class ErrorResponse(
    val statusCode: Int,
    val message: String,
    val error: String? = null,
)
