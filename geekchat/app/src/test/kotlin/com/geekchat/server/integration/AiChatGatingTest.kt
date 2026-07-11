package com.geekchat.server.integration

import com.geekchat.server.ai.application.port.out.AiChatPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

/**
 * Admin-gating proof for AI chat (Phase 5): only ADMIN reaches POST /api/ai/chat.
 * Uses a @Primary stub AiChatPort so the gate is verified deterministically, independent of
 * any real provider/API key (the actual model call is exercised in production with a key).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
@Import(AiChatGatingTest.StubAiConfig::class)
class AiChatGatingTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @TestConfiguration
    class StubAiConfig {
        @Bean
        @Primary
        fun stubAiChatPort(): AiChatPort = object : AiChatPort {
            override fun reply(prompt: String): String = "stub-reply"
        }
    }

    private fun devLoginAccessToken(name: String, admin: Boolean): String {
        val body = mockMvc.get("/auth/dev-login?name=$name&admin=$admin")
            .andReturn().response.contentAsString
        return objectMapper.readTree(body).get("accessToken").asText()
    }

    @Test
    fun `admin can use AI chat`() {
        val token = devLoginAccessToken("AiAdmin", admin = true)
        mockMvc.post("/api/ai/chat") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"summarize this room"}"""
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `normal user is forbidden from AI chat`() {
        val token = devLoginAccessToken("AiUser", admin = false)
        mockMvc.post("/api/ai/chat") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"hi"}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `anonymous is unauthorized for AI chat`() {
        mockMvc.post("/api/ai/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"hi"}"""
        }.andExpect { status { isUnauthorized() } }
    }
}
