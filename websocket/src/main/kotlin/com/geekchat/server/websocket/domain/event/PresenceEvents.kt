package com.geekchat.server.websocket.domain.event

data class UserConnected(
    val userId: String,
    val sessionId: String,
)

data class UserDisconnected(
    val userId: String,
    val sessionId: String,
)
