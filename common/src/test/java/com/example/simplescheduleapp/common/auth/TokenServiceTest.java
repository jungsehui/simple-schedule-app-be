package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenService 은(는)")
class TokenServiceTest {

    // HS512 요구사항(64바이트 이상)을 충족하는 테스트 전용 시크릿 (실제 운영 값 아님)
    private static final String TEST_SECRET = Base64.getUrlEncoder().encodeToString(
            "this-is-a-test-only-secret-key-for-token-service-unit-tests-64bytes!!".getBytes());

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(new TokenProperty(TEST_SECRET, 3_600_000L));
    }

    @DisplayName("발급한 토큰에서 memberId와 role을 그대로 복원한다")
    @Test
    void roundTripMemberIdAndRole() {
        Token token = tokenService.createToken(1L, Role.TUTOR);

        assertThat(tokenService.extractMemberId(token.accessToken())).isEqualTo(1L);
        assertThat(tokenService.extractRole(token.accessToken())).isEqualTo(Role.TUTOR);
    }

    @DisplayName("role claim이 없는 과거 토큰은 role을 null로 반환한다 (Phase 1 이전 발급분 호환)")
    @Test
    void legacyTokenWithoutRoleClaimReturnsNullRole() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(TEST_SECRET));
        String legacyToken = Jwts.builder()
                .claim("memberId", 2L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        assertThat(tokenService.extractMemberId(legacyToken)).isEqualTo(2L);
        assertThat(tokenService.extractRole(legacyToken)).isNull();
    }

    @DisplayName("만료된 토큰은 EXPIRED_TOKEN 예외를 던진다")
    @Test
    void expiredTokenThrows() {
        TokenService shortLived = new TokenService(new TokenProperty(TEST_SECRET, -1_000L));
        Token token = shortLived.createToken(3L, Role.STUDENT);

        assertThatThrownBy(() -> tokenService.extractMemberId(token.accessToken()))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("위조/손상된 토큰은 예외를 던진다")
    @Test
    void malformedTokenThrows() {
        assertThatThrownBy(() -> tokenService.extractMemberId("not-a-jwt"))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("memberId claim이 없으면 sub에서 식별자를 복원한다 (계정 통합 토큰 관용 파서 — ADR-0003 Stage 0)")
    @Test
    void unifiedTokenWithSubOnlyFallsBackToSubject() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(TEST_SECRET));
        String unifiedToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        assertThat(tokenService.extractMemberId(unifiedToken)).isEqualTo(42L);
    }

    @DisplayName("숫자가 아닌 sub(GeekChat UUID 토큰 등)는 예외 없이 null을 반환한다")
    @Test
    void nonNumericSubjectReturnsNull() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(TEST_SECRET));
        String uuidSubToken = Jwts.builder()
                .subject("3f6c1b2a-8d4e-4c5f-9a7b-000000000000")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        assertThat(tokenService.extractMemberId(uuidSubToken)).isNull();
    }
}
