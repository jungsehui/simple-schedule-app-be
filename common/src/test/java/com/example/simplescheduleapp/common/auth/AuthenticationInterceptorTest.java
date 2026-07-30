package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 인증 게이트 — <b>기본 차단</b> 규칙 (ADR-0005).
 *
 * <p>이 테스트의 핵심은 마지막 케이스다: 아무 애노테이션도 없는 평범한 핸들러가 <b>거부</b>되는지.
 * 그게 성립해야 "새 엔드포인트를 추가하면 기본이 보호"라는 보증이 사실이 된다.
 */
@DisplayName("인증 게이트 은(는)")
@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private BearerTokenExtractor bearerTokenExtractor;

    @InjectMocks
    private AuthenticationInterceptor authenticationInterceptor;

    static class ProtectedController {
        public void plain() {
        }

        @PublicEndpoint
        public void open() {
        }
    }

    @PublicEndpoint
    static class OpenController {
        public void anything() {
        }
    }

    private HandlerMethod handler(Object controller, String methodName) throws NoSuchMethodException {
        return new HandlerMethod(controller, controller.getClass().getMethod(methodName));
    }

    @DisplayName("애노테이션이 없는 평범한 핸들러는 토큰이 없으면 거부한다 — 기본이 보호다")
    @Test
    void plainHandlerWithoutTokenIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), handler(new ProtectedController(), "plain")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.REQUIRED_BEARER_TOKEN);
    }

    @DisplayName("유효한 토큰이 있으면 통과한다")
    @Test
    void validTokenPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        given(bearerTokenExtractor.extract("Bearer token")).willReturn("token");
        given(tokenService.extractMemberId("token")).willReturn(7L);

        boolean result = authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), handler(new ProtectedController(), "plain"));

        assertThat(result).isTrue();
    }

    @DisplayName("회원 식별자가 없는 토큰(다른 서비스 발급 등)은 거부한다")
    @Test
    void tokenWithoutMemberIdIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer foreign");
        given(bearerTokenExtractor.extract("Bearer foreign")).willReturn("foreign");
        given(tokenService.extractMemberId("foreign")).willReturn(null);

        assertThatThrownBy(() -> authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), handler(new ProtectedController(), "plain")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.INVALID_TOKEN);
    }

    @DisplayName("@PublicEndpoint 메서드는 토큰 없이 통과한다")
    @Test
    void publicMethodPassesWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), handler(new ProtectedController(), "open"));

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }

    @DisplayName("@PublicEndpoint 클래스의 메서드는 토큰 없이 통과한다")
    @Test
    void publicClassPassesWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), handler(new OpenController(), "anything"));

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }

    @DisplayName("컨트롤러가 아닌 핸들러(정적 리소스 등)는 검사하지 않는다")
    @Test
    void nonHandlerMethodIsSkipped() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = authenticationInterceptor.preHandle(
                request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }
}
