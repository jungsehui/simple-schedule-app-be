package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Base64;

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
}
