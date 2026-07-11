package com.geekchat.server.ai.infrastructure

import com.geekchat.server.ai.application.port.out.AiChatPort
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * Spring AI implementation of [AiChatPort]. Resolves the auto-configured [ChatModel] (the provider
 * selected by `spring.ai.model.chat`) lazily via [ObjectProvider] at call time.
 *
 * Why ObjectProvider instead of @ConditionalOnBean: this is a component-scanned @Component, and
 * component scanning runs BEFORE Spring AI's auto-configuration registers the ChatModel. A
 * scan-time `@ConditionalOnBean(ChatModel)` would therefore never see the model and the adapter
 * would never be created — even with a valid API key. Late-bound resolution sidesteps that ordering
 * trap entirely.
 *
 * When no provider/API key is configured there is no ChatModel bean and this reports AI chat as
 * unavailable, so the app still boots and human-to-human chat is unaffected. Swapping providers is
 * config only (`spring.ai.model.chat`); this class is unchanged.
 */
@Component
class SpringAiChatAdapter(
    private val chatModelProvider: ObjectProvider<ChatModel>,
) : AiChatPort {

    override fun reply(prompt: String): String {
        val chatModel = chatModelProvider.getIfAvailable()
            ?: return "AI chat is not configured. Set an AI provider + API key (spring.ai.*)."
        return ChatClient.create(chatModel)
            .prompt()
            .user(prompt)
            .call()
            .content() ?: ""
    }
}
