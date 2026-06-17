package com.geekchat.server.adapter.`in`.websocket

import com.geekchat.server.application.port.out.WebSocketBroadcaster
import com.geekchat.server.domain.event.ChatEvent
import com.geekchat.server.chat.domain.model.MessageType
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Characterization tests for ChatEventListener.
 *
 * These tests pin the exact WS broadcast payload shapes so that an upcoming
 * Spring Modulith refactor (shared ChatEvent split + sync→async conversion)
 * cannot silently alter what clients receive.
 *
 * No Spring context is loaded — the listener is constructed directly with a
 * relaxed MockK broadcaster and each listener method is called synchronously.
 */
class ChatEventListenerTest {

    private val broadcaster = mockk<WebSocketBroadcaster>(relaxed = true)
    private lateinit var listener: ChatEventListener

    @BeforeEach
    fun setUp() {
        listener = ChatEventListener(broadcaster)
    }

    // -----------------------------------------------------------------------
    // onMessageSent — the critical payload: must include content, reply, burn
    // -----------------------------------------------------------------------

    @Test
    fun `onMessageSent broadcasts new_message with all required fields`() {
        val createdAt = Instant.parse("2024-01-15T10:00:00Z")
        val expiresAt = Instant.parse("2024-01-15T10:05:00Z")
        val event = ChatEvent.MessageSent(
            messageId = "msg-1",
            roomId = "room-1",
            senderId = "user-1",
            content = "Hello, World!",
            messageType = MessageType.TEXT,
            createdAt = createdAt,
            clientMessageId = "client-uuid-1",
            expiresAt = expiresAt,
            replyToMessageId = "msg-0",
            burnAfterRead = true,
        )

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageSent(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-1", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("new_message", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("msg-1", data["id"])
        assertEquals("room-1", data["roomId"])
        assertEquals("user-1", data["senderId"])
        assertEquals("Hello, World!", data["content"])
        assertEquals("TEXT", data["type"])
        assertEquals(createdAt.toString(), data["createdAt"])
        assertEquals(expiresAt.toString(), data["expiresAt"])
        assertEquals("msg-0", data["replyToMessageId"])
        assertEquals(true, data["burnAfterRead"])
    }

    @Test
    fun `onMessageSent with nullable optional fields broadcasts null for expiresAt and replyToMessageId`() {
        val event = ChatEvent.MessageSent(
            messageId = "msg-2",
            roomId = "room-2",
            senderId = "user-2",
            content = "plain text",
            messageType = MessageType.TEXT,
            createdAt = Instant.parse("2024-01-16T09:00:00Z"),
            clientMessageId = "client-uuid-2",
            // expiresAt and replyToMessageId default to null, burnAfterRead defaults to false
        )

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageSent(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-2", message = capture(msgSlot))
        }

        @Suppress("UNCHECKED_CAST")
        val data = msgSlot.captured.data as Map<String, Any?>
        assertNull(data["expiresAt"])
        assertNull(data["replyToMessageId"])
        assertEquals(false, data["burnAfterRead"])
    }

    @Test
    fun `onMessageSent with SYSTEM messageType broadcasts type as SYSTEM`() {
        val event = ChatEvent.MessageSent(
            messageId = "msg-sys",
            roomId = "room-3",
            senderId = "system",
            content = "User joined",
            messageType = MessageType.SYSTEM,
            createdAt = Instant.now(),
            clientMessageId = "client-uuid-sys",
        )

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageSent(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-3", message = capture(msgSlot))
        }

        @Suppress("UNCHECKED_CAST")
        val data = msgSlot.captured.data as Map<String, Any?>
        assertEquals("SYSTEM", data["type"])
    }

    // -----------------------------------------------------------------------
    // onMessageBurned
    // -----------------------------------------------------------------------

    @Test
    fun `onMessageBurned broadcasts message_burned with roomId and messageId`() {
        val event = ChatEvent.MessageBurned(roomId = "room-1", messageId = "msg-burn")

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageBurned(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-1", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("message_burned", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("room-1", data["roomId"])
        assertEquals("msg-burn", data["messageId"])
    }

    // -----------------------------------------------------------------------
    // onMessageRead
    // -----------------------------------------------------------------------

    @Test
    fun `onMessageRead broadcasts read_update with roomId, userId, lastReadAt`() {
        val lastReadAt = Instant.parse("2024-03-01T12:30:00Z")
        val event = ChatEvent.MessageRead(roomId = "room-4", userId = "user-4", lastReadAt = lastReadAt)

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageRead(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-4", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("read_update", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("room-4", data["roomId"])
        assertEquals("user-4", data["userId"])
        assertEquals(lastReadAt.toString(), data["lastReadAt"])
    }

    // -----------------------------------------------------------------------
    // onMessageExpired
    // -----------------------------------------------------------------------

    @Test
    fun `onMessageExpired broadcasts message_expired with roomId and messageIds list`() {
        val messageIds = listOf("msg-a", "msg-b", "msg-c")
        val event = ChatEvent.MessageExpired(roomId = "room-5", messageIds = messageIds)

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageExpired(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-5", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("message_expired", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("room-5", data["roomId"])
        assertEquals(messageIds, data["messageIds"])
    }

    @Test
    fun `onMessageExpired with empty messageIds list broadcasts empty list`() {
        val event = ChatEvent.MessageExpired(roomId = "room-6", messageIds = emptyList())

        val msgSlot = slot<WsOutMessage>()
        listener.onMessageExpired(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-6", message = capture(msgSlot))
        }

        @Suppress("UNCHECKED_CAST")
        val data = msgSlot.captured.data as Map<String, Any?>
        assertEquals(emptyList<String>(), data["messageIds"])
    }

    // -----------------------------------------------------------------------
    // onRoomExpiring
    // -----------------------------------------------------------------------

    @Test
    fun `onRoomExpiring broadcasts room_expiring with roomId, roomName, expiresAt`() {
        val expiresAt = Instant.parse("2024-06-01T00:00:00Z")
        val event = ChatEvent.RoomExpiring(roomId = "room-7", roomName = "Dev Chat", expiresAt = expiresAt)

        val msgSlot = slot<WsOutMessage>()
        listener.onRoomExpiring(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-7", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("room_expiring", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("room-7", data["roomId"])
        assertEquals("Dev Chat", data["roomName"])
        assertEquals(expiresAt.toString(), data["expiresAt"])
    }

    @Test
    fun `onRoomExpiring with null roomName broadcasts null roomName`() {
        val expiresAt = Instant.parse("2024-06-01T00:00:00Z")
        val event = ChatEvent.RoomExpiring(roomId = "room-8", roomName = null, expiresAt = expiresAt)

        val msgSlot = slot<WsOutMessage>()
        listener.onRoomExpiring(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-8", message = capture(msgSlot))
        }

        @Suppress("UNCHECKED_CAST")
        val data = msgSlot.captured.data as Map<String, Any?>
        assertNull(data["roomName"])
    }

    // -----------------------------------------------------------------------
    // onRoomExpired
    // -----------------------------------------------------------------------

    @Test
    fun `onRoomExpired broadcasts room_expired with roomId and roomName`() {
        val event = ChatEvent.RoomExpired(roomId = "room-9", roomName = "Old Room")

        val msgSlot = slot<WsOutMessage>()
        listener.onRoomExpired(event)

        verify(exactly = 1) {
            broadcaster.broadcastToRoom(roomId = "room-9", message = capture(msgSlot))
        }

        val captured = msgSlot.captured
        assertEquals("room_expired", captured.type)

        @Suppress("UNCHECKED_CAST")
        val data = captured.data as Map<String, Any?>
        assertEquals("room-9", data["roomId"])
        assertEquals("Old Room", data["roomName"])
    }
}
