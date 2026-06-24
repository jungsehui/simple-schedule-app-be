package com.geekchat.server.ai.application.port.out

/**
 * Provider-agnostic AI chat abstraction. Implemented over Spring AI's ChatClient so the
 * underlying model (Anthropic / OpenAI / Ollama / …) can be swapped via configuration
 * (`spring.ai.model.chat`) without touching application code.
 */
interface AiChatPort {
    fun reply(prompt: String): String
}
