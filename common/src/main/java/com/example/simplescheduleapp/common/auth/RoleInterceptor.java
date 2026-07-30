package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * {@link RequireRole} 인가 인터셉터 (ADR-0005).
 *
 * <p>{@link AuthenticationInterceptor}가 먼저 실행돼 토큰 유효성을 보장하므로 여기서는 역할만
 * 따진다. 역할 클레임이 없는 토큰도 거부한다 — 인가 판단 근거가 없는 토큰을 통과시키는 것은
 * fail-open이기 때문이다(Phase 3a에서는 과거 토큰 호환을 위해 통과시켰다).
 *
 * <p>{@code @RequireRole}이 없는 엔드포인트는 "인증은 필요하지만 역할 제한은 없다"는 뜻이다.
 * 무인증 허용은 오직 {@link PublicEndpoint}로만 표현한다.
 */
@RequiredArgsConstructor
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final BearerTokenExtractor bearerTokenExtractor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = bearerTokenExtractor.extract(bearerToken);
        Role role = tokenService.extractRole(token);
        if (role == null) {
            // 역할 클레임이 없으면 이 엔드포인트를 호출할 자격이 있는지 판단할 근거가 없다 — 재로그인 필요.
            throw new ApplicationException(TokenExceptionCode.FORBIDDEN);
        }

        boolean hasRole = Arrays.asList(requireRole.value()).contains(role);
        if (!hasRole) {
            throw new ApplicationException(TokenExceptionCode.FORBIDDEN);
        }

        return true;
    }
}
