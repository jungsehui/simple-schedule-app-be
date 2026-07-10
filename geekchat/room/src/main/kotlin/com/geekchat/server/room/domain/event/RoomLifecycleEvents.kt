package com.geekchat.server.room.domain.event

import java.time.Instant

data class RoomExpiring(
    val roomId: String,
    val roomName: String?,
    val expiresAt: Instant,
)

data class RoomExpired(
    val roomId: String,
    val roomName: String?,
)
