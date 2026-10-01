package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * prod DB(Supabase PostgreSQL, ADR-0003 Stage 3b 스모크와 같은 16)에서 겹침 조회 네이티브 쿼리를 검증한다.
 * Docker가 필요하다.
 */
@Testcontainers
class ScheduleOverlapQueryPostgreSqlTest extends AbstractScheduleOverlapQueryDialectTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    // application-test.yml이 H2Dialect를 고정하므로 엔진에 맞게 덮어쓴다.
    @DynamicPropertySource
    static void dialect(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
