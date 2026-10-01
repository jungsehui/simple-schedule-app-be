package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 로컬 dev DB(MySQL 8.0, 루트 docker-compose.yml과 같은 이미지)에서 겹침 조회 네이티브 쿼리를 검증한다.
 * Docker가 필요하다.
 */
@Testcontainers
class ScheduleOverlapQueryMySqlTest extends AbstractScheduleOverlapQueryDialectTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    // application-test.yml이 H2Dialect를 고정하므로 엔진에 맞게 덮어쓴다.
    @DynamicPropertySource
    static void dialect(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }
}
