package com.geekchat.server.integration

import com.geekchat.server.room.infrastructure.persistence.entity.ChatRoomJpaEntity
import com.geekchat.server.room.infrastructure.persistence.entity.ChatRoomMemberJpaEntity
import com.geekchat.server.chat.infrastructure.persistence.entity.MessageJpaEntity
import com.geekchat.server.user.domain.model.UserRole
import com.geekchat.server.user.infrastructure.persistence.entity.UserJpaEntity
import com.geekchat.server.room.infrastructure.persistence.repository.SpringDataChatRoomMemberRepository
import com.geekchat.server.room.infrastructure.persistence.repository.SpringDataChatRoomRepository
import com.geekchat.server.chat.infrastructure.persistence.repository.SpringDataMessageRepository
import com.geekchat.server.user.infrastructure.persistence.repository.SpringDataUserRepository
import com.geekchat.server.room.domain.model.ChatRoomType
import com.geekchat.server.chat.domain.model.MessageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoundTripMappingTest {

    @Autowired lateinit var userRepo: SpringDataUserRepository
    @Autowired lateinit var chatRoomRepo: SpringDataChatRoomRepository
    @Autowired lateinit var memberRepo: SpringDataChatRoomMemberRepository
    @Autowired lateinit var messageRepo: SpringDataMessageRepository

    @Test
    fun `User round-trip preserves all fields`() {
        val entity = UserJpaEntity(
            id = UUID.randomUUID().toString(),
            nickname = "Alice",
            username = "alice_123",
            email = "alice@test.com",
            profileImageUrl = "https://img.test/alice.jpg",
            role = UserRole.ADMIN,
        )
        val saved = userRepo.saveAndFlush(entity)
        val loaded = userRepo.findById(saved.id).get()
        val domain = loaded.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals("Alice", domain.nickname)
        assertEquals("alice_123", domain.username)
        assertEquals("alice@test.com", domain.email)
        assertEquals("https://img.test/alice.jpg", domain.profileImageUrl)
        assertEquals(UserRole.ADMIN, domain.role)
        assertNotNull(domain.createdAt)
        assertNotNull(domain.updatedAt)
        assertNull(domain.deletedAt)
    }

    @Test
    fun `ChatRoom round-trip preserves all fields`() {
        val entity = ChatRoomJpaEntity(
            id = UUID.randomUUID().toString(),
            type = ChatRoomType.GROUP,
            name = "Test Group",
        )
        val saved = chatRoomRepo.saveAndFlush(entity)
        val loaded = chatRoomRepo.findById(saved.id).get()
        val domain = loaded.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(ChatRoomType.GROUP, domain.type)
        assertEquals("Test Group", domain.name)
        assertNull(domain.lastMessageAt)
        assertNull(domain.deletedAt)
    }

    @Test
    fun `Message round-trip preserves all fields`() {
        val user = userRepo.saveAndFlush(UserJpaEntity(id = UUID.randomUUID().toString(), nickname = "Sender"))
        val room = chatRoomRepo.saveAndFlush(ChatRoomJpaEntity(id = UUID.randomUUID().toString(), type = ChatRoomType.DIRECT))
        val clientMsgId = UUID.randomUUID().toString()

        val entity = MessageJpaEntity(
            id = UUID.randomUUID().toString(),
            chatRoom = room,
            sender = user,
            clientMessageId = clientMsgId,
            content = "Hello, World!",
            type = MessageType.TEXT,
            replyToMessageId = "parent-msg-id",
            burnAfterRead = true,
        )
        val saved = messageRepo.saveAndFlush(entity)
        val loaded = messageRepo.findById(saved.id).get()
        val domain = loaded.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(room.id, domain.chatRoomId)
        assertEquals(user.id, domain.senderId)
        assertEquals(clientMsgId, domain.clientMessageId)
        assertEquals("Hello, World!", domain.content)
        assertEquals(MessageType.TEXT, domain.type)
        assertEquals("parent-msg-id", domain.replyToMessageId)
        assertEquals(true, domain.burnAfterRead)
        assertNull(domain.deletedAt)
    }

    @Test
    fun `ChatRoomMember round-trip preserves all fields including muted`() {
        val user = userRepo.saveAndFlush(UserJpaEntity(id = UUID.randomUUID().toString(), nickname = "Member"))
        val room = chatRoomRepo.saveAndFlush(ChatRoomJpaEntity(id = UUID.randomUUID().toString(), type = ChatRoomType.DIRECT))

        val entity = ChatRoomMemberJpaEntity(
            id = UUID.randomUUID().toString(),
            user = user,
            chatRoom = room,
            muted = true,
        )
        val saved = memberRepo.saveAndFlush(entity)
        val loaded = memberRepo.findById(saved.id).get()
        val domain = loaded.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(user.id, domain.userId)
        assertEquals(room.id, domain.chatRoomId)
        assertEquals(true, domain.muted)
        assertNotNull(domain.joinedAt)
        assertNull(domain.lastReadAt)
    }
}
