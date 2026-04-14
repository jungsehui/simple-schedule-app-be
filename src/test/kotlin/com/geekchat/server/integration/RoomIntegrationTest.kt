package com.geekchat.server.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
class RoomIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun loginAndGetToken(name: String): Pair<String, String> {
        val result = mockMvc.get("/auth/dev-login?name=$name")
            .andReturn().response.contentAsString
        val node = objectMapper.readTree(result)
        val token = node.get("accessToken").asText()

        val meResult = mockMvc.get("/auth/me") {
            header("Authorization", "Bearer $token")
        }.andReturn().response.contentAsString
        val userId = objectMapper.readTree(meResult).get("id").asText()
        return Pair(token, userId)
    }

    @Test
    fun `create DIRECT room and list rooms`() {
        val (token1, _) = loginAndGetToken("RoomUser1")
        val (_, userId2) = loginAndGetToken("RoomUser2")

        // Create room
        val createResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.type") { value("DIRECT") }
            jsonPath("$.members.length()") { value(2) }
        }.andReturn().response.contentAsString

        val roomId = objectMapper.readTree(createResult).get("id").asText()

        // List rooms
        mockMvc.get("/api/rooms") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(roomId) }
            jsonPath("$[0].type") { value("DIRECT") }
            jsonPath("$[0].members.length()") { value(2) }
            jsonPath("$[0].members[0].userId") { isNotEmpty() }
            jsonPath("$[0].members[0].nickname") { isNotEmpty() }
        }
    }

    @Test
    fun `create DIRECT room is idempotent`() {
        val (token1, _) = loginAndGetToken("IdempUser1")
        val (_, userId2) = loginAndGetToken("IdempUser2")

        val result1 = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andReturn().response.contentAsString

        val result2 = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andReturn().response.contentAsString

        val id1 = objectMapper.readTree(result1).get("id").asText()
        val id2 = objectMapper.readTree(result2).get("id").asText()
        assert(id1 == id2) { "DIRECT room should be idempotent" }
    }

    @Test
    fun `get messages returns empty for new room`() {
        val (token1, _) = loginAndGetToken("MsgUser1")
        val (_, userId2) = loginAndGetToken("MsgUser2")

        val createResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andReturn().response.contentAsString

        val roomId = objectMapper.readTree(createResult).get("id").asText()

        mockMvc.get("/api/rooms/$roomId/messages") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `get messages for non-member returns 403`() {
        val (token1, _) = loginAndGetToken("NonMem1")
        val (_, userId2) = loginAndGetToken("NonMem2")
        val (token3, _) = loginAndGetToken("NonMem3")

        val createResult = mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andReturn().response.contentAsString

        val roomId = objectMapper.readTree(createResult).get("id").asText()

        mockMvc.get("/api/rooms/$roomId/messages") {
            header("Authorization", "Bearer $token3")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `rooms require authentication`() {
        mockMvc.get("/api/rooms")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
