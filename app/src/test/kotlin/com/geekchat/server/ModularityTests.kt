package com.geekchat.server

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Verifies the Spring Modulith feature-module structure: each top-level package
 * under com.geekchat.server (common, user, auth, room, chat, websocket) is an
 * application module; modules may only reference each other's public API +
 * declared allowed dependencies. Fails the build on any boundary violation.
 */
class ModularityTests {

    private val modules = ApplicationModules.of(GeekChatServerApplication::class.java)

    @Test
    fun `verify module structure`() {
        modules.verify()
    }
}
