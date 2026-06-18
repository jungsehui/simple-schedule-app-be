package com.geekchat.server.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

/**
 * End-to-end proof of the admin gate (Phase 4): JWT role claim → JwtAuthenticationFilter
 * authority → @PreAuthorize. Uses a test-only admin-gated endpoint so the chain is exercised
 * without shipping a production endpoint (the real admin-gated AI endpoint arrives in Phase 5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
@Import(AdminGatingIntegrationTest.TestAdminController::class)
class AdminGatingIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @RestController
    class TestAdminController {
        @GetMapping("/test/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        fun adminOnly(): Map<String, String> = mapOf("ok" to "admin")
    }

    private fun devLoginAccessToken(name: String, admin: Boolean): String {
        val body = mockMvc.get("/auth/dev-login?name=$name&admin=$admin")
            .andReturn().response.contentAsString
        return objectMapper.readTree(body).get("accessToken").asText()
    }

    @Test
    fun `admin token reaches admin-only endpoint`() {
        val token = devLoginAccessToken("AdminUser", admin = true)
        mockMvc.get("/test/admin-only") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `normal user token is forbidden from admin-only endpoint`() {
        val token = devLoginAccessToken("NormalUser", admin = false)
        mockMvc.get("/test/admin-only") { header("Authorization", "Bearer $token") }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `anonymous request is unauthorized for admin-only endpoint`() {
        mockMvc.get("/test/admin-only")
            .andExpect { status { isUnauthorized() } }
    }
}
