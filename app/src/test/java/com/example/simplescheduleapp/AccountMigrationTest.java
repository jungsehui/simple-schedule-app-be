package com.example.simplescheduleapp;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * account 백필(V4)과 GeekChat 연결(V5)의 정합성 (ADR-0006 P1).
 *
 * <p>운영과 같은 순서를 흉내 낸다: V3까지 적용된 스키마에 회원과 GeekChat 사용자가 이미 있고, 그 위에 V4, V5가
 * 돈다. 시드를 버전 사이에 넣어야 해서 Spring 컨텍스트 대신 Flyway API를 직접 쓴다.
 * 데이터베이스를 시나리오마다 따로 만들어 서로 간섭하지 않게 한다. Docker가 필요하다.
 */
@Testcontainers
class AccountMigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("SSA 회원은 같은 id로, 겹치는 GeekChat 사용자는 PENDING 링크로, GeekChat 전용은 새 id로 옮기고 이후 가입 id와 충돌하지 않는다")
    void 백필_정합성() {
        DataSource dataSource = newDatabase("backfill");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        flyway(dataSource).target("3").load().migrate();

        // SSA: 옛 규칙의 대문자 username, 소프트 삭제 회원, id 사이의 빈 번호
        insertMember(jdbc, 1, "OldUser", null);
        insertMember(jdbc, 2, "ssaonly", null);
        insertMember(jdbc, 7, "gone", "2026-01-01 00:00:00");

        // GeekChat: 겹치는 사용자, 전용 사용자, 탈퇴(익명화)한 사용자
        jdbc.execute("CREATE SCHEMA geekchat");
        jdbc.execute("""
                CREATE TABLE geekchat.users (
                    id varchar(36) PRIMARY KEY, nickname varchar(20) NOT NULL, username varchar(20) UNIQUE,
                    email varchar(255), profile_image_url varchar(1024), password_hash varchar(255),
                    status varchar(20) NOT NULL, role varchar(20) NOT NULL,
                    created_at timestamp(6) with time zone NOT NULL, updated_at timestamp(6) with time zone NOT NULL,
                    deleted_at timestamp(6) with time zone)""");
        insertGeekChatUser(jdbc, "11111111-1111-1111-1111-111111111111", "olduser", "ACTIVE");
        insertGeekChatUser(jdbc, "22222222-2222-2222-2222-222222222222", "gconly", "ACTIVE");
        insertGeekChatUser(jdbc, "33333333-3333-3333-3333-333333333333", null, "WITHDRAWN");

        flyway(dataSource).load().migrate();

        // SSA 회원 3명이 같은 id로, external_uuid 없이
        assertThat(jdbc.queryForList(
                "SELECT id FROM account WHERE external_uuid IS NULL ORDER BY id", Long.class))
                .containsExactly(1L, 2L, 7L);
        assertThat(jdbc.queryForObject("SELECT username FROM account WHERE id = 1", String.class))
                .isEqualTo("olduser");
        Map<String, Object> withdrawn = jdbc.queryForMap("SELECT username, status, withdrawn_at FROM account WHERE id = 7");
        assertThat(withdrawn.get("username")).isNull();
        assertThat(withdrawn.get("status")).isEqualTo("WITHDRAWN");
        assertThat(withdrawn.get("withdrawn_at")).isNotNull();

        // 겹치는 사용자: PENDING 링크만, users.account_id는 비어 있다
        assertThat(jdbc.queryForList(
                "SELECT account_id || ':' || status FROM account_link WHERE external_uuid = '11111111-1111-1111-1111-111111111111'",
                String.class)).containsExactly("1:PENDING");
        assertThat(jdbc.queryForObject(
                "SELECT account_id FROM geekchat.users WHERE id = '11111111-1111-1111-1111-111111111111'", Long.class))
                .isNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM account WHERE external_uuid = '11111111-1111-1111-1111-111111111111'", Integer.class))
                .isZero();

        // GeekChat 전용: SSA id보다 큰 새 id, users.account_id 채움
        Long gcOnlyId = jdbc.queryForObject(
                "SELECT id FROM account WHERE external_uuid = '22222222-2222-2222-2222-222222222222'", Long.class);
        assertThat(gcOnlyId).isGreaterThan(7L);
        assertThat(jdbc.queryForObject(
                "SELECT account_id FROM geekchat.users WHERE id = '22222222-2222-2222-2222-222222222222'", Long.class))
                .isEqualTo(gcOnlyId);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM account WHERE external_uuid = '33333333-3333-3333-3333-333333333333'", String.class))
                .isEqualTo("WITHDRAWN");

        // 백필 뒤 SSA 가입(id 미지정 INSERT)은 GeekChat 전용 계정의 id를 다시 받지 않는다
        Long newMemberId = jdbc.queryForObject("""
                INSERT INTO member (role, age, name, password, phone_number, username)
                VALUES ('STUDENT', 20, 'n', 'p', '010', 'newbie') RETURNING member_id""", Long.class);
        assertThat(newMemberId).isGreaterThan(gcOnlyId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM account WHERE id = ?", Integer.class, newMemberId)).isZero();
    }

    @Test
    @DisplayName("geekchat 스키마가 없으면 V5는 아무것도 하지 않고 성공한다")
    void geekchat_스키마가_없으면_V5는_건너뛴다() {
        DataSource dataSource = newDatabase("nogeekchat");

        flyway(dataSource).load().migrate();

        List<String> applied = new JdbcTemplate(dataSource).queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(applied).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("geekchat 스키마는 있는데 users를 찾지 못하면 조용히 넘어가지 않고 실패한다")
    void geekchat_users를_못_찾으면_실패한다() {
        DataSource dataSource = newDatabase("nousers");
        new JdbcTemplate(dataSource).execute("CREATE SCHEMA geekchat");

        assertThatThrownBy(() -> flyway(dataSource).load().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("geekchat.users");
    }

    private static DataSource newDatabase(String name) {
        new JdbcTemplate(dataSource(POSTGRES.getJdbcUrl())).execute("CREATE DATABASE " + name);
        return dataSource(POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + name));
    }

    private static DataSource dataSource(String url) {
        return new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration");
    }

    private static void insertMember(JdbcTemplate jdbc, long id, String username, String deletedDate) {
        jdbc.update("""
                INSERT INTO member (role, member_id, created_date, updated_date, deleted_date, age, name, password, phone_number, username)
                VALUES ('STUDENT', ?, now(), now(), CAST(? AS timestamp), 20, 'n', 'p', ?, ?)""",
                id, deletedDate, "010" + id, username);
    }

    private static void insertGeekChatUser(JdbcTemplate jdbc, String id, String username, String status) {
        jdbc.update("""
                INSERT INTO geekchat.users (id, nickname, username, status, role, created_at, updated_at)
                VALUES (?, 'nick', ?, ?, 'USER', now(), now())""", id, username, status);
    }
}
