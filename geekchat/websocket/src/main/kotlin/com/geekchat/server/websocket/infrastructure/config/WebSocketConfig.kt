package com.geekchat.server.websocket.infrastructure.config

import com.geekchat.server.websocket.presentation.ChatWebSocketHandler
import com.geekchat.server.common.config.AppProperties
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
        // REST CORS(SecurityConfig)와 동일한 허용 목록을 쓴다.
        // 이전에는 frontendUrl 단일값만 허용해, 같은 앱의 REST는 되는데 WS만
        // 403으로 끊기는 비대칭이 있었다(로컬 개발 origin·Vercel 프리뷰 등).
        // setAllowedOriginPatterns는 와일드카드(https://*.vercel.app)도 지원한다.
        val patterns = (listOf(appProperties.frontendUrl) + appProperties.frontendOriginPatterns)
            .filter { it.isNotBlank() }

        registry
            .addHandler(chatWebSocketHandler, "/ws")
            .setAllowedOriginPatterns(*patterns.toTypedArray())
    }
}
