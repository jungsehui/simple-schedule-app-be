package com.geekchat.server.websocket.infrastructure

import tools.jackson.databind.ObjectMapper
import com.geekchat.server.websocket.application.port.out.WebSocketBroadcaster
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Component
class WebSocketBroadcasterAdapter(
    private val sessionManager: WebSocketSessionManager,
    private val objectMapper: ObjectMapper,
) : WebSocketBroadcaster {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun broadcastToRoom(roomId: String, message: Any, excludeUserId: String?) {
        val json = objectMapper.writeValueAsString(message)
        val textMessage = TextMessage(json)
        sessionManager.getSessionsForRoom(roomId, excludeUserId).forEach { session ->
            sendSafe(session, textMessage)
        }
    }

    override fun broadcastToUser(userId: String, message: Any) {
        val json = objectMapper.writeValueAsString(message)
        val textMessage = TextMessage(json)
        sessionManager.getSessionsForUser(userId).forEach { session ->
            sendSafe(session, textMessage)
        }
    }

    override fun isUserOnline(userId: String): Boolean = sessionManager.isUserOnline(userId)

    override fun joinRoom(userId: String, roomId: String) {
        sessionManager.joinRoom(userId, roomId)
    }

    private fun sendSafe(session: WebSocketSession, message: TextMessage) {
        try {
            if (session.isOpen) {
                synchronized(session) {
                    session.sendMessage(message)
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to send message to session={}: {}", session.id, e.message)
        }
    }
}
