package com.geekchat.server.ai.infrastructure

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.ObjectProvider

/**
 * Unit test for the call-time fallback branch (advisor Issue 1): when no provider is configured
 * the ObjectProvider yields no ChatModel, and the adapter must report AI chat as unavailable
 * instead of failing. The model-present branch is a thin ChatClient delegation exercised in
 * production with a real API key.
 */
class SpringAiChatAdapterTest {

    @Test
    fun `reports unavailable when no ChatModel is configured`() {
        val provider = mockk<ObjectProvider<ChatModel>>()
        every { provider.getIfAvailable() } returns null

        val reply = SpringAiChatAdapter(provider).reply("hello")

        assertTrue(reply.contains("not configured"), "expected the unavailable message, got: $reply")
    }
}
