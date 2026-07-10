package com.geekchat.server.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

/**
 * Phase 7 (HATEOAS): room responses gain `_links` while keeping the existing JSON contract.
 * Also proves Spring HATEOAS 3.1.1 HAL serialization works on Boot 4.1 + Jackson 3 (tools.jackson):
 * if the HAL module were missing/incompatible, `_links` would not render.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
class RoomHateoasTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun login(name: String): Pair<String, String> {
        val body = mockMvc.get("/auth/dev-login?name=$name").andReturn().response.contentAsString
        val token = objectMapper.readTree(body).get("accessToken").asText()
        val me = mockMvc.get("/auth/me") { header("Authorization", "Bearer $token") }
            .andReturn().response.contentAsString
        return token to objectMapper.readTree(me).get("id").asText()
    }

    @Test
    fun `create room keeps fields and adds hypermedia links`() {
        val (token1, _) = login("HateoasUser1")
        val (_, userId2) = login("HateoasUser2")

        mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andExpect {
            status { isOk() }
            // existing contract preserved (additive wrapping)
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.type") { value("DIRECT") }
            jsonPath("$.members.length()") { value(2) }
            // new: hypermedia links
            jsonPath("$._links.self.href") { isNotEmpty() }
            jsonPath("$._links.mute.href") { isNotEmpty() }
            jsonPath("$._links['invite-link'].href") { isNotEmpty() }
        }
    }

    @Test
    fun `list rooms stays a plain array with no _embedded wrapper`() {
        val (token1, _) = login("HateoasList1")
        val (_, userId2) = login("HateoasList2")

        mockMvc.post("/api/rooms") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = """{"memberIds":["$userId2"]}"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/rooms") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isOk() }
            // Contract preserved: top-level array, NOT wrapped in a CollectionModel `_embedded` object.
            jsonPath("$[0].id") { isNotEmpty() }
            jsonPath("$[0].type") { value("DIRECT") }
            jsonPath("$._embedded") { doesNotExist() }
        }
    }
}
