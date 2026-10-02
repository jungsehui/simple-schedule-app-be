package com.example.simplescheduleapp;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 스키마 기준선(Flyway V1~)과 엔티티가 맞는지 검증한다 (ADR-0006 결정 6).
 *
 * <p>빈 PostgreSQL에 Flyway 마이그레이션을 전부 적용한 뒤, Hibernate {@code ddl-auto: validate}로
 * 전체 컨텍스트를 띄운다. 엔티티가 요구하는 테이블이나 컬럼이 기준선에 없거나 타입이 다르면 컨텍스트 기동이
 * 실패한다. 운영은 이 기준선과 같은 스키마에서 {@code validate}로 기동하므로, 운영 기동 실패를 CI에서 먼저 잡는다.
 *
 * <p>스키마 변경을 V4 이상 마이그레이션 없이 엔티티만 바꿔 배포하는 실수도 이 테스트가 막는다.
 * Docker가 필요하다 (없으면 스킵이 아니라 실패한다).
 */
@Testcontainers
@SpringBootTest
@EmbeddedKafka(partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class SchemaBaselineValidationTest {

    // 운영 DB(Supabase PostgreSQL)와 같은 메이저
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void dialect(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    // FcmConfig(@Profile("!test"))가 test에선 FirebaseMessaging을 만들지 않으므로 모킹
    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway 기준선으로 만든 스키마에서 엔티티 검증(validate)을 통과해 컨텍스트가 기동한다")
    void 기준선_스키마와_엔티티가_일치한다() {
        // 컨텍스트 기동 성공 자체가 validate 통과의 증거다. 아래는 Flyway가 실제로 돌았는지 확인한다.
        List<String> applied = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(applied).containsExactly("1", "2", "3");
    }
}
