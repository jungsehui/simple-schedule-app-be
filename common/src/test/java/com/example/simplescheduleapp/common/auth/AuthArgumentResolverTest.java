package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("AuthArgumentResolver 은(는)")
class AuthArgumentResolverTest {

    private static final String TEST_SECRET = Base64.getUrlEncoder().encodeToString(
            "this-is-a-test-only-secret-key-for-auth-resolver-unit-tests-64bytes!!".getBytes());

    private TokenService tokenService;
    private AuthArgumentResolver resolver;
    private NativeWebRequest webRequest;
    private HttpServletRequest httpRequest;

    // MethodParameter 픽스처: 실제 애노테이션 인스턴스를 리플렉션으로 얻기 위한 더미 핸들러
    @SuppressWarnings("unused")
    static class Fixture {
        void requiredAuth(@Auth Long memberId) {
        }

        void optionalAuth(@Auth(required = false) Long memberId) {
        }

        void requiredRole(@Auth Role role) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenService = new TokenService(new TokenProperty(TEST_SECRET, 3_600_000L));
        resolver = new AuthArgumentResolver(tokenService, new BearerTokenExtractor());

        webRequest = mock(NativeWebRequest.class);
        httpRequest = mock(HttpServletRequest.class);
        given(webRequest.getNativeRequest()).willReturn(httpRequest);
    }

    private MethodParameter param(String methodName) throws Exception {
        return MethodParameter.forExecutable(Fixture.class.getDeclaredMethod(methodName, Long.class), 0);
    }

    private MethodParameter roleParam() throws Exception {
        return MethodParameter.forExecutable(Fixture.class.getDeclaredMethod("requiredRole", Role.class), 0);
    }

    /**
     * 역할 클레임이 <b>없는</b> 토큰을 만든다.
     *
     * <p>{@code TokenService.createToken}은 role을 항상 넣으므로 그 경로로는 만들 수 없다.
     * 같은 키로 직접 서명해 서명은 유효하되 role만 빠진 토큰을 재현한다 — Phase 1 이전에
     * 발급된 토큰이 이 모양이었다.
     */
    private String tokenWithoutRoleClaim(long memberId) {
        return Jwts.builder()
                .claim("memberId", memberId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(TEST_SECRET)), Jwts.SIG.HS512)
                .compact();
    }

    @DisplayName("@Auth Long 파라미터를 지원한다")
    @Test
    void supportsAuthAnnotatedLongParameter() throws Exception {
        assertThat(resolver.supportsParameter(param("requiredAuth"))).isTrue();
    }

    @DisplayName("유효한 Bearer 토큰이면 memberId를 주입한다")
    @Test
    void resolvesMemberIdFromValidToken() throws Exception {
        Token token = tokenService.createToken(7L, Role.STUDENT);
        given(httpRequest.getHeader("Authorization")).willReturn("Bearer " + token.accessToken());

        Object resolved = resolver.resolveArgument(param("requiredAuth"), null, webRequest, null);

        assertThat(resolved).isEqualTo(7L);
    }

    @DisplayName("required=true(기본)에서 토큰이 없으면 예외를 던진다")
    @Test
    void requiredAuthWithoutTokenThrows() throws Exception {
        given(httpRequest.getHeader("Authorization")).willReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(param("requiredAuth"), null, webRequest, null))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("required=false에서 토큰이 없으면 null을 주입한다 (optional-auth)")
    @Test
    void optionalAuthWithoutTokenReturnsNull() throws Exception {
        given(httpRequest.getHeader("Authorization")).willReturn(null);

        Object resolved = resolver.resolveArgument(param("optionalAuth"), null, webRequest, null);

        assertThat(resolved).isNull();
    }

    @DisplayName("required=false에서 유효하지 않은 토큰이면 null을 주입한다 (optional-auth)")
    @Test
    void optionalAuthWithInvalidTokenReturnsNull() throws Exception {
        given(httpRequest.getHeader("Authorization")).willReturn("Bearer invalid-token");

        Object resolved = resolver.resolveArgument(param("optionalAuth"), null, webRequest, null);

        assertThat(resolved).isNull();
    }

    @DisplayName("required=true에서 유효하지 않은 토큰이면 예외를 던진다")
    @Test
    void requiredAuthWithInvalidTokenThrows() throws Exception {
        given(httpRequest.getHeader("Authorization")).willReturn("Bearer invalid-token");

        assertThatThrownBy(() -> resolver.resolveArgument(param("requiredAuth"), null, webRequest, null))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("@Auth Role 파라미터도 지원한다 — 애노테이션은 하나, 타입이 무엇을 뽑을지 정한다")
    @Test
    void supportsAuthAnnotatedRoleParameter() throws Exception {
        assertThat(resolver.supportsParameter(roleParam())).isTrue();
    }

    /**
     * 역할 부재는 {@code RoleInterceptor}와 <b>같은 코드</b>여야 한다.
     *
     * <p>이 경로는 {@code @RequireRole} 없이 {@code @Auth Role}만 쓰는 엔드포인트
     * ({@code GET /me/schedules})가 탄다. 인터셉터의 그물에 안 걸리는 두 번째 지점이라,
     * 여기가 다른 코드를 내면 클라이언트는 같은 원인에 두 규칙을 만들어야 한다.
     */
    @DisplayName("역할 클레임이 없는 토큰은 인터셉터와 같은 T8을 던진다")
    @Test
    void roleClaimMissingThrowsSameCodeAsInterceptor() throws Exception {
        given(httpRequest.getHeader("Authorization"))
                .willReturn("Bearer " + tokenWithoutRoleClaim(7L));

        assertThatThrownBy(() -> resolver.resolveArgument(roleParam(), null, webRequest, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.REQUIRED_ROLE_CLAIM);
    }

    @DisplayName("역할이 있는 토큰은 Role을 주입한다")
    @Test
    void resolvesRoleFromValidToken() throws Exception {
        Token token = tokenService.createToken(7L, Role.TUTOR);
        given(httpRequest.getHeader("Authorization")).willReturn("Bearer " + token.accessToken());

        Object resolved = resolver.resolveArgument(roleParam(), null, webRequest, null);

        assertThat(resolved).isEqualTo(Role.TUTOR);
    }
}
