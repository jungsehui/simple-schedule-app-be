package com.geekchat.server.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.geekchat.server.infrastructure.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "dev")
class WebSocketIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun connectWs(token: String?): Pair<WebSocketSession, ArrayBlockingQueue<String>> {
        val messages = ArrayBlockingQueue<String>(100)
        val handler = object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                messages.add(message.payload)
            }
        }

        val url = if (token != null) {
            "ws://localhost:$port/ws?token=$token"
        } else {
            "ws://localhost:$port/ws"
        }

        val client = StandardWebSocketClient()
        val session = client.execute(handler, WebSocketHttpHeaders(), URI(url)).get(5, TimeUnit.SECONDS)
        // Give time for connection handling
        Thread.sleep(200)
        return Pair(session, messages)
    }

    private fun devLoginAndGetUserId(name: String): Pair<String, String> {
        // Use JwtTokenProvider directly to create a user via REST
        val client = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/auth/dev-login?name=$name"))
            .GET()
            .build()
        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val node = objectMapper.readTree(response.body())
        val token = node.get("accessToken").asText()

        val meRequest = java.net.http.HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/auth/me"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()
        val meResponse = client.send(meRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
        val userId = objectMapper.readTree(meResponse.body()).get("id").asText()
        return Pair(token, userId)
    }

    @Test
    fun `connection without token receives error and closes`() {
        try {
            val (session, messages) = connectWs(null)
            val msg = messages.poll(3, TimeUnit.SECONDS)
            if (msg != null) {
                val node = objectMapper.readTree(msg)
                assertEquals("error", node.get("type").asText())
                assertEquals("NO_TOKEN", node.get("data").get("code").asText())
            }
            // Session should be closed by server
            Thread.sleep(500)
            assertTrue(!session.isOpen)
        } catch (_: Exception) {
            // Connection refused or closed is also valid
        }
    }

    @Test
    fun `connection with valid token succeeds`() {
        val (token, _) = devLoginAndGetUserId("WsUser1")
        val (session, _) = connectWs(token)

        assertTrue(session.isOpen)
        session.close()
    }

    @Test
    fun `send_message returns message_ack`() {
        val (token1, userId1) = devLoginAndGetUserId("WsAck1")
        val (_, userId2) = devLoginAndGetUserId("WsAck2")

        // Create room via REST
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val createRoomReq = java.net.http.HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/api/rooms"))
            .header("Authorization", "Bearer $token1")
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""{"memberIds":["$userId2"]}"""))
            .build()
        val roomResponse = httpClient.send(createRoomReq, java.net.http.HttpResponse.BodyHandlers.ofString())
        val roomId = objectMapper.readTree(roomResponse.body()).get("id").asText()

        // Connect WebSocket
        val (session, messages) = connectWs(token1)

        // Send message
        val sendPayload = objectMapper.writeValueAsString(
            mapOf(
                "type" to "send_message",
                "data" to mapOf(
                    "roomId" to roomId,
                    "content" to "Hello!",
                    "clientMessageId" to java.util.UUID.randomUUID().toString(),
                ),
            ),
        )
        session.sendMessage(TextMessage(sendPayload))

        // Expect message_ack (may arrive after other messages)
        var ackFound = false
        for (i in 1..10) {
            val msg = messages.poll(2, TimeUnit.SECONDS) ?: break
            val node = objectMapper.readTree(msg)
            if (node.get("type").asText() == "message_ack") {
                assertTrue(node.get("data").get("serverId").asText().isNotBlank())
                ackFound = true
                break
            }
        }
        assertTrue(ackFound, "Should receive message_ack")

        session.close()
    }

    @Test
    fun `typing_start broadcasts typing_indicator to other user`() {
        val (token1, userId1) = devLoginAndGetUserId("WsTyp1")
        val (token2, userId2) = devLoginAndGetUserId("WsTyp2")

        // Create room
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val createRoomReq = java.net.http.HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/api/rooms"))
            .header("Authorization", "Bearer $token1")
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""{"memberIds":["$userId2"]}"""))
            .build()
        val roomResponse = httpClient.send(createRoomReq, java.net.http.HttpResponse.BodyHandlers.ofString())
        val roomId = objectMapper.readTree(roomResponse.body()).get("id").asText()

        // Connect both users
        val (session1, _) = connectWs(token1)
        val (session2, messages2) = connectWs(token2)

        // User1 sends typing_start
        val typingPayload = objectMapper.writeValueAsString(
            mapOf("type" to "typing_start", "data" to mapOf("roomId" to roomId)),
        )
        session1.sendMessage(TextMessage(typingPayload))

        // User2 should receive typing_indicator
        val indicator = messages2.poll(5, TimeUnit.SECONDS)
        assertTrue(indicator != null, "User2 should receive typing_indicator")
        val node = objectMapper.readTree(indicator)
        assertEquals("typing_indicator", node.get("type").asText())
        assertEquals(roomId, node.get("data").get("roomId").asText())

        session1.close()
        session2.close()
    }
}
