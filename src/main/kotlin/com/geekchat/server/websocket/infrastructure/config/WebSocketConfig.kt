package com.geekchat.server.websocket.infrastructure.config

import com.geekchat.server.websocket.presentation.ChatWebSocketHandler
import com.geekchat.server.infrastructure.config.AppProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val chatWebSocketHandler: ChatWebSocketHandler,
    private val appProperties: AppProperties,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(chatWebSocketHandler, "/ws")
            .setAllowedOrigins(appProperties.frontendUrl)
    }
}
