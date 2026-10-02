package com.example.simplescheduleapp.member.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 통합 신원 {@code account}의 JPA 영속 모델 (ADR-0006 P1). 스키마 정본은 Flyway {@code V4__account.sql}이다.
 *
 * <p>P1에서는 SSA 가입 때 쓰기만 한다. {@code id}는 {@code member_id}를 그대로 받으므로 생성 전략이 없다.
 * 비밀번호 해시는 P2에서 옮긴다(V4 주석 참고). 그래서 이 엔티티에는 아직 해시를 쓰는 경로가 없다.
 */
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class AccountEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "external_uuid", length = 36, unique = true)
    private String externalUuid;

    @Column(name = "username", length = 20, unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    public static AccountEntity activeMember(Long memberId, String username, Instant now) {
        AccountEntity account = new AccountEntity();
        account.id = memberId;
        account.username = username;
        account.status = "ACTIVE";
        account.createdAt = now;
        account.updatedAt = now;
        return account;
    }
}
