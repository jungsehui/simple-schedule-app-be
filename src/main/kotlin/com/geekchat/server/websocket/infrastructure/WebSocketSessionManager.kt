package com.geekchat.server.websocket.infrastructure

import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketSessionManager {

    private val userSessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
    private val sessionToUser = ConcurrentHashMap<String, String>()
    private val userRooms = ConcurrentHashMap<String, MutableSet<String>>()
    private val lastConnectionTime = ConcurrentHashMap<String, Long>()

    companion object {
        const val MAX_CONNECTIONS_PER_USER = 3
        const val MIN_CONNECTION_INTERVAL_MS = 2000L
    }

    fun register(userId: String, session: WebSocketSession): RegistrationResult {
        val now = System.currentTimeMillis()
        val lastTime = lastConnectionTime.get(userId)
        if (lastTime != null && (now - lastTime) < MIN_CONNECTION_INTERVAL_MS) {
            return RegistrationResult.RateLimited
        }

        val sessions = userSessions.computeIfAbsent(userId) {
            ConcurrentHashMap.newKeySet()
        }
        if (sessions.size >= MAX_CONNECTIONS_PER_USER) {
            return RegistrationResult.MaxConnectionsExceeded
        }

        sessions.add(session)
        sessionToUser[session.id] = userId
        lastConnectionTime[userId] = now
        return RegistrationResult.Success
    }

    fun unregister(session: WebSocketSession) {
        val userId = sessionToUser.remove(session.id) ?: return
        val sessions = userSessions[userId] ?: return
        sessions.remove(session)
        if (sessions.isEmpty()) {
            userSessions.remove(userId)
            userRooms.remove(userId)
        }
    }

    fun getUserId(session: WebSocketSession): String? = sessionToUser[session.id]

    fun joinRoom(userId: String, roomId: String) {
        userRooms.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(roomId)
    }

    fun getUserRooms(userId: String): Set<String> = userRooms[userId] ?: emptySet()

    fun getSessionsForUser(userId: String): Set<WebSocketSession> =
        userSessions[userId]?.filter { it.isOpen }?.toSet() ?: emptySet()

    fun getSessionsForRoom(roomId: String, excludeUserId: String? = null): Set<WebSocketSession> {
        val result = mutableSetOf<WebSocketSession>()
        for ((userId, rooms) in userRooms) {
            if (userId == excludeUserId) continue
            if (rooms.contains(roomId)) {
                userSessions[userId]?.filter { it.isOpen }?.let { result.addAll(it) }
            }
        }
        return result
    }

    fun isUserOnline(userId: String): Boolean =
        userSessions[userId]?.any { it.isOpen } == true

    fun getTotalConnections(): Int = sessionToUser.size

    sealed class RegistrationResult {
        data object Success : RegistrationResult()
        data object RateLimited : RegistrationResult()
        data object MaxConnectionsExceeded : RegistrationResult()
    }
}
