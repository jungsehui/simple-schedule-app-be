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
 * 인가 인터셉터 규칙 (ADR-0005 — 기본 차단으로 전환된 이후).
 *
 * <p>이전 Phase 3a는 무토큰 요청과 role 클레임이 없는 토큰을 통과시켰다. 두 통과 경로는
 * 인가 판단 근거 없이 보호 엔드포인트를 열어주는 fail-open이라 폐기했다.
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
    void 무토큰_요청은_거부한다() throws Exception {
        // AuthenticationInterceptor가 앞서 막지만, 이 인터셉터 단독으로도 열리지 않아야 한다(다층 방어).
        MockHttpServletRequest request = new MockHttpServletRequest();
        given(bearerTokenExtractor.extract(null))
                .willThrow(new ApplicationException(TokenExceptionCode.REQUIRED_BEARER_TOKEN));

        assertThatThrownBy(() -> roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.REQUIRED_BEARER_TOKEN);
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

    /**
     * 역할 <b>부재</b>는 역할 <b>불일치</b>와 다른 코드를 낸다.
     *
     * <p>둘 다 403이지만 클라이언트가 취해야 할 행동이 정반대다 — 부재는 재인증(판단 근거가 없는
     * 토큰), 불일치는 세션 유지 + "권한 없음" 표시다. 전에는 둘 다 T6이라 구분할 수 없었고,
     * 그래서 "403이면 재인증"으로 처리하면 정상 로그인한 TUTOR가 STUDENT 전용 엔드포인트를
     * 호출하는 순간 강제 로그아웃됐다.
     */
    @Test
    void role_클레임이_없는_토큰은_불일치와_다른_코드를_낸다() throws Exception {
        // 역할을 모르면 이 엔드포인트를 호출할 자격이 있는지 판단할 근거가 없다 — 통과시키면 fail-open.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy");
        given(bearerTokenExtractor.extract("Bearer legacy")).willReturn("legacy");
        given(tokenService.extractRole("legacy")).willReturn(null);

        assertThatThrownBy(() -> roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("tutorOnly")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.REQUIRED_ROLE_CLAIM);
    }

    /**
     * 두 원인이 같은 코드로 되돌아가는 회귀를 막는다.
     *
     * <p>위 두 테스트가 각각 자기 코드를 단정하지만, 그것만으로는 <b>둘이 서로 달라야 한다</b>는
     * 성질이 고정되지 않는다. 누군가 양쪽을 같은 값으로 되돌리면 개별 단정도 함께 바뀌어 통과할
     * 수 있다. 이 테스트는 그 경우에 실패한다.
     */
    @Test
    void 역할_부재와_역할_불일치는_서로_다른_코드여야_한다() {
        assertThat(TokenExceptionCode.REQUIRED_ROLE_CLAIM.getCode())
                .isNotEqualTo(TokenExceptionCode.FORBIDDEN.getCode());
    }

    @Test
    void RequireRole이_없는_핸들러는_역할을_검사하지_않는다() throws Exception {
        // 인증은 AuthenticationInterceptor가 이미 보장했다 — 여기서는 역할 제한이 없다는 뜻일 뿐이다.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        boolean result = roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("open"));

        assertThat(result).isTrue();
        verifyNoInteractions(tokenService, bearerTokenExtractor);
    }
}
