package com.geekchat.server.adapter.`in`.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.time.Instant
import javax.sql.DataSource

@RestController
class HealthController(
    private val dataSource: DataSource,
) {
    @GetMapping("/health")
    fun check(): ResponseEntity<Map<String, Any>> {
        val dbStatus = try {
            dataSource.connection.use { conn ->
                conn.createStatement().use { it.execute("SELECT 1") }
            }
            "connected"
        } catch (_: Exception) {
            "disconnected"
        }

        val status = if (dbStatus == "connected") "ok" else "degraded"
        val uptimeSeconds = ManagementFactory.getRuntimeMXBean().uptime / 1000.0

        return ResponseEntity.ok(
            mapOf(
                "status" to status,
                "db" to dbStatus,
                "uptime" to uptimeSeconds,
                "timestamp" to Instant.now().toString(),
            ),
        )
    }
}
