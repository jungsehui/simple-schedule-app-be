package com.geekchat.server.integration

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
class InviteLinkIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun loginAndGetToken(name: String): Pair<String, String> {
        val result = mockMvc.get("/auth/dev-login?name=$name").andReturn().response.contentAsString
        val node = objectMapper.readTree(result)
        val token = node.get("accessToken").asText()
        val meResult = mockMvc.get("/auth/me") { header("Authorization", "Bearer $token") }.andReturn().response.contentAsString
        val userId = objectMapper.readTree(meResult).get("id").asText()
        return Pair(token, userId)
    }

    @Test
    fun `create invite link and join room`() {
        val (token1, _) = loginAndGetToken("InvUser1")
        val (_, userId2) = loginAndGetToken("InvUser2")
        val (token3, _) = loginAndGetToken("InvUser3")

        // Create GROUP room (need 2+ members or name)
        val roomResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"],"name":"InviteGroup"}"""
        }.andReturn().response.contentAsString
        val roomId = objectMapper.readTree(roomResult).get("id").asText()

        // Create invite link
        val linkResult = mockMvc.post("/api/rooms/$roomId/invite-link") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ttlHours":24}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { isNotEmpty() }
            jsonPath("$.roomId") { value(roomId) }
        }.andReturn().response.contentAsString
        val code = objectMapper.readTree(linkResult).get("code").asText()

        // User3 joins via invite link
        mockMvc.post("/api/invite/$code/join") {
            header("Authorization", "Bearer $token3")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(roomId) }
            jsonPath("$.members.length()") { value(3) }
        }
    }

    @Test
    fun `join by code when already member returns 409`() {
        val (token1, userId1) = loginAndGetToken("AlreadyMem1")
        val (_, userId2) = loginAndGetToken("AlreadyMem2")

        val roomResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"],"name":"AlreadyGroup"}"""
        }.andReturn().response.contentAsString
        val roomId = objectMapper.readTree(roomResult).get("id").asText()

        val linkResult = mockMvc.post("/api/rooms/$roomId/invite-link") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andReturn().response.contentAsString
        val code = objectMapper.readTree(linkResult).get("code").asText()

        // User1 is already a member — should get 409
        mockMvc.post("/api/invite/$code/join") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `create invite link requires room membership`() {
        val (token1, _) = loginAndGetToken("LinkNonMem1")
        val (_, userId2) = loginAndGetToken("LinkNonMem2")
        val (token3, _) = loginAndGetToken("LinkNonMem3")

        val roomResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"],"name":"Private"}"""
        }.andReturn().response.contentAsString
        val roomId = objectMapper.readTree(roomResult).get("id").asText()

        // User3 is NOT a member — should get 403
        mockMvc.post("/api/rooms/$roomId/invite-link") {
            header("Authorization", "Bearer $token3")
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `create room with ttlHours returns expiresAt`() {
        val (token1, _) = loginAndGetToken("TtlUser1")
        val (_, userId2) = loginAndGetToken("TtlUser2")

        mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"],"name":"Ephemeral","ttlHours":24}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.expiresAt") { isNotEmpty() }
            jsonPath("$.type") { value("GROUP") }
        }
    }
}
