package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
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
 * Phase 3a(듀얼리드) 인가 인터셉터 규칙 테스트:
 * 무토큰 통과 / 유효 토큰 role 일치 통과 / role 불일치 403 / 레거시(role 없는) 토큰 통과.
 */
@ExtendWith(MockitoExtension.class)
class RoleInterceptorTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private BearerTokenExtractor bearerTokenExtractor;

    @InjectMocks
    private RoleInterceptor roleInterceptor;

    static class TestController {
        @RequireRole(Role.TUTOR)
        public void tutorOnly() {
        }

        public void open() {
        }
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    @Test
    void 무토큰_요청은_통과한다() throws Exception {
        // Phase 3a: 레거시 클라이언트 허용 — 3b에서 401로 전환
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly"));

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }

    @Test
    void 역할이_일치하는_토큰은_통과한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        given(bearerTokenExtractor.extract("Bearer token")).willReturn("token");
        given(tokenService.extractRole("token")).willReturn(Role.TUTOR);

        boolean result = roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly"));

        assertThat(result).isTrue();
    }

    @Test
    void 역할이_불일치하는_토큰은_403_FORBIDDEN() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        given(bearerTokenExtractor.extract("Bearer token")).willReturn("token");
        given(tokenService.extractRole("token")).willReturn(Role.STUDENT);

        assertThatThrownBy(() -> roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.FORBIDDEN);
    }

    @Test
    void role_클레임이_없는_레거시_토큰은_통과한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy");
        given(bearerTokenExtractor.extract("Bearer legacy")).willReturn("legacy");
        given(tokenService.extractRole("legacy")).willReturn(null);

        boolean result = roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly"));

        assertThat(result).isTrue();
    }

    @Test
    void RequireRole이_없는_핸들러는_검사하지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        boolean result = roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("open"));

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }
}
