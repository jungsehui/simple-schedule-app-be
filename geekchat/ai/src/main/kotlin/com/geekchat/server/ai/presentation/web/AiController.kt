package com.geekchat.server.ai.presentation.web

import com.geekchat.server.ai.application.service.AiChatService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AiChatRequest(val message: String = "")
data class AiChatResponse(val reply: String)

/**
 * Admin-only AI chat. Non-admin / anonymous callers never reach this (Phase 4 gating:
 * @PreAuthorize("hasRole('ADMIN')")). Human-to-human chat (WebSocket) is unaffected.
 */
@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiChatService: AiChatService,
) {
    @PostMapping("/chat")
    @PreAuthorize("hasRole('ADMIN')")
    fun chat(@RequestBody request: AiChatRequest): AiChatResponse =
        AiChatResponse(reply = aiChatService.chat(request.message))
}
