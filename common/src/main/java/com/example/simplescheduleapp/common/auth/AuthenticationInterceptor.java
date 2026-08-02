package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증 게이트 — <b>기본 차단</b>(ADR-0005).
 *
 * <p>모든 핸들러 요청에 유효한 Bearer 토큰을 요구하고, {@link PublicEndpoint}가 붙은 곳만 통과시킨다.
 * 이전에는 {@code @RequireRole}이 붙은 엔드포인트만 검사하는 fail-open 구조여서, 애노테이션을
 * 빠뜨린 7개 엔드포인트가 조용히 무방비로 남아 있었다(수강생 명단 노출 등).
 *
 * <p>{@code /internal/**}은 {@code InternalApiKeyFilter}(공유 시크릿)가 담당하므로
 * {@code AuthConfig}에서 이 인터셉터의 대상에서 제외한다 — 서버 간 호출은 회원 토큰을 갖지 않는다.
 *
 * <p>역할 검사는 {@link RoleInterceptor}가 이어서 수행한다. 인증(누구인가)과 인가(무엇을 할 수
 * 있는가)를 분리해 각각 한 가지 이유로만 바뀌게 한다.
 */
@RequiredArgsConstructor
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final BearerTokenExtractor bearerTokenExtractor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 정적 리소스·에러 디스패치 등 컨트롤러가 아닌 핸들러
            return true;
        }

        if (isPublic(handlerMethod)) {
            return true;
        }

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null) {
            throw new ApplicationException(TokenExceptionCode.REQUIRED_BEARER_TOKEN);
        }

        String token = bearerTokenExtractor.extract(bearerToken);
        Long memberId = tokenService.extractMemberId(token);
        if (memberId == null) {
            // 서명은 유효하지만 SSA 회원 식별자가 없는 토큰(예: GeekChat UUID sub) — 이 서비스의 주체가 아니다.
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        }

        return true;
    }

    private boolean isPublic(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PublicEndpoint.class)
                || handlerMethod.getBeanType().isAnnotationPresent(PublicEndpoint.class);
    }
}
