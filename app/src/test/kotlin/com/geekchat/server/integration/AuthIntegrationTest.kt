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
class AuthIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `dev-login returns tokens and message`() {
        mockMvc.get("/auth/dev-login?name=TestUser")
            .andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { isNotEmpty() }
                jsonPath("$.refreshToken") { isNotEmpty() }
                jsonPath("$.message") { value("Logged in as TestUser") }
            }
    }

    @Test
    fun `dev-login without name returns 400`() {
        mockMvc.get("/auth/dev-login")
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `auth me returns user info with valid token`() {
        val loginResult = mockMvc.get("/auth/dev-login?name=MeUser")
            .andReturn().response.contentAsString
        val token = objectMapper.readTree(loginResult).get("accessToken").asText()

        mockMvc.get("/auth/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.nickname") { value("MeUser") }
            jsonPath("$.id") { isNotEmpty() }
        }
    }

    @Test
    fun `auth me returns 401 without token`() {
        mockMvc.get("/auth/me")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `refresh token returns new token pair`() {
        val loginResult = mockMvc.get("/auth/dev-login?name=RefreshUser")
            .andReturn().response.contentAsString
        val refreshToken = objectMapper.readTree(loginResult).get("refreshToken").asText()

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"$refreshToken"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.refreshToken") { isNotEmpty() }
        }
    }

    @Test
    fun `refresh with invalid token returns 401`() {
        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"nonexistent-token"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `logout succeeds`() {
        val loginResult = mockMvc.get("/auth/dev-login?name=LogoutUser")
            .andReturn().response.contentAsString
        val refreshToken = objectMapper.readTree(loginResult).get("refreshToken").asText()

        mockMvc.post("/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"$refreshToken"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }
}
