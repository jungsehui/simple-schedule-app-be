package com.geekchat.server.adapter.`in`.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.geekchat.server.application.port.out.ChatRoomMemberRepository
import com.geekchat.server.application.service.MessageService
import com.geekchat.server.domain.error.Either
import com.geekchat.server.domain.event.ChatEvent
import com.geekchat.server.infrastructure.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class ChatWebSocketHandler(
    private val objectMapper: ObjectMapper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val sessionManager: WebSocketSessionManager,
    private val broadcaster: WebSocketBroadcasterAdapter,
    private val messageService: MessageService,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── Connection lifecycle ──

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val token = extractToken(session)
        if (token == null) {
            sendError(session, "NO_TOKEN", "Token not provided")
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        val tokenResult = jwtTokenProvider.validateToken(token)
        if (tokenResult.isLeft) {
            val error = (tokenResult as Either.Left).value
            val code = when {
                error.message.contains("expired", ignoreCase = true) -> "TOKEN_EXPIRED"
                else -> "INVALID_TOKEN"
            }
            sendError(session, code, error.message)
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        val userId = tokenResult.getOrNull()!!

        when (sessionManager.register(userId, session)) {
            WebSocketSessionManager.RegistrationResult.RateLimited -> {
                sendError(session, "RATE_LIMITED", "Reconnecting too fast")
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }
            WebSocketSessionManager.RegistrationResult.MaxConnectionsExceeded -> {
                sendError(session, "MAX_CONNECTIONS", "Max ${WebSocketSessionManager.MAX_CONNECTIONS_PER_USER} connections per user")
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }
            WebSocketSessionManager.RegistrationResult.Success -> {}
        }

        val members = chatRoomMemberRepository.findAllByUserId(userId)
        members.forEach { member -> sessionManager.joinRoom(userId, member.chatRoomId) }

        eventPublisher.publishEvent(ChatEvent.UserConnected(userId, session.id))

        log.info(
            "ws_connected userId={} socketId={} roomCount={} totalConnections={}",
            userId, session.id, members.size, sessionManager.getTotalConnections(),
        )
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = sessionManager.getUserId(session)
        sessionManager.unregister(session)

        if (userId != null) {
            eventPublisher.publishEvent(ChatEvent.UserDisconnected(userId, session.id))
            log.info(
                "ws_disconnected userId={} socketId={} totalConnections={}",
                userId, session.id, sessionManager.getTotalConnections(),
            )
        }
    }

    // ── Message routing ──

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val userId = sessionManager.getUserId(session)
        if (userId == null) {
            sendError(session, "NOT_AUTHENTICATED", "Not authenticated")
            return
        }

        val node = try {
            objectMapper.readTree(message.payload)
        } catch (_: Exception) {
            sendError(session, "INVALID_FORMAT", "Invalid JSON")
            return
        }

        val type = node.get("type")?.asText()
        val data = node.get("data")

        when (type) {
            "send_message" -> handleSendMessage(session, userId, data)
            "typing_start" -> handleTypingStart(session, userId, data)
            "mark_read" -> handleMarkRead(session, userId, data)
            else -> sendError(session, "UNKNOWN_EVENT", "Unknown event type: $type")
        }
    }

    // ── Event handlers ──

    private fun handleSendMessage(session: WebSocketSession, userId: String, data: JsonNode?) {
        val roomId = data?.get("roomId")?.asText()
            ?: return sendError(session, "INVALID_DATA", "roomId is required")
        val content = data.get("content")?.asText()
            ?: return sendError(session, "INVALID_DATA", "content is required")
        val clientMessageId = data.get("clientMessageId")?.asText()
            ?: return sendError(session, "INVALID_DATA", "clientMessageId is required")

        val startTime = System.currentTimeMillis()

        messageService.sendMessage(roomId, userId, content, clientMessageId).fold(
            onLeft = { error -> sendError(session, "SEND_FAILED", error.message) },
            onRight = { msg ->
                sendToSession(
                    session,
                    WsOutMessage("message_ack", mapOf("clientMessageId" to clientMessageId, "serverId" to msg.id)),
                )
                log.info(
                    "message_handled roomId={} senderId={} messageId={} duration_ms={}",
                    roomId, userId, msg.id, System.currentTimeMillis() - startTime,
                )
            },
        )
    }

    private fun handleTypingStart(session: WebSocketSession, userId: String, data: JsonNode?) {
        val roomId = data?.get("roomId")?.asText() ?: return

        broadcaster.broadcastToRoom(
            roomId,
            WsOutMessage("typing_indicator", mapOf("roomId" to roomId, "userId" to userId)),
            excludeUserId = userId,
        )
    }

    private fun handleMarkRead(session: WebSocketSession, userId: String, data: JsonNode?) {
        val roomId = data?.get("roomId")?.asText() ?: return
        val lastReadMessageId = data.get("lastReadMessageId")?.asText() ?: return

        messageService.markAsRead(roomId, userId, lastReadMessageId)
    }

    // ── Helpers ──

    private fun extractToken(session: WebSocketSession): String? {
        val query = session.uri?.query ?: return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .find { it[0] == "token" }
            ?.getOrNull(1)
    }

    private fun sendError(session: WebSocketSession, code: String, message: String) {
        sendToSession(session, WsOutMessage("error", mapOf("code" to code, "message" to message)))
    }

    private fun sendToSession(session: WebSocketSession, message: Any) {
        val json = objectMapper.writeValueAsString(message)
        try {
            if (session.isOpen) {
                synchronized(session) {
                    session.sendMessage(TextMessage(json))
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to send message to session={}: {}", session.id, e.message)
        }
    }
}

data class WsOutMessage(
    val type: String,
    val data: Any,
)
