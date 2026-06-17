package com.geekchat.server

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Phase 3 probe: confirm Spring Modulith resolves + runs on Spring Boot 4.1,
 * and surface what modules it currently detects from the flat layer layout.
 * Does NOT call verify() yet (the current layer-based packages are expected to
 * violate module rules — the restructuring into feature modules comes next).
 */
class ModularityTests {

    @Test
    fun `print detected application modules`() {
        val modules = ApplicationModules.of(GeekChatServerApplication::class.java)
        println("=== DETECTED MODULES ===")
        modules.forEach { println("MODULE: $it") }
        println("=== END ===")
    }
}
