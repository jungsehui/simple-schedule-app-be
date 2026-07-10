package com.geekchat.server.ai.application.service

import com.geekchat.server.ai.application.port.out.AiChatPort
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val aiChatPort: AiChatPort,
) {
    fun chat(prompt: String): String = aiChatPort.reply(prompt)
}
