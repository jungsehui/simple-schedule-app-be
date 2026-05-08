package com.geekchat.server.application.port.out

interface WebSocketBroadcaster {
    fun broadcastToRoom(roomId: String, message: Any, excludeUserId: String? = null)
    fun broadcastToUser(userId: String, message: Any)
    fun isUserOnline(userId: String): Boolean
    fun joinRoom(userId: String, roomId: String)
}
