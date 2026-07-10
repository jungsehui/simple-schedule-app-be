package com.geekchat.server.chat.application

import com.geekchat.server.chat.domain.repository.MessageRepository
import com.geekchat.server.room.domain.event.RoomExpired
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class RoomExpiryMessageCleanupTest {

    private val messageRepository = mockk<MessageRepository>(relaxed = true)
    private val cleanup = RoomExpiryMessageCleanup(messageRepository)

    @Test
    fun `onRoomExpired soft-deletes the expired room's messages`() {
        cleanup.onRoomExpired(RoomExpired(roomId = "r1", roomName = "Expired"))

        verify { messageRepository.softDeleteByRoomId("r1", any()) }
    }
}
