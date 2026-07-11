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
 * {@link RequireRole} 인가 인터셉터 — 인증 마이그레이션 Phase 3a(듀얼리드) 규칙:
 * <ul>
 *   <li>Authorization 헤더 부재 → 통과 (레거시 클라이언트 — 3b에서 401로 전환)</li>
 *   <li>토큰 제시 + 파싱 실패 → 401 (제시된 토큰은 엄격 검증 — 조용히 무시하지 않음)</li>
 *   <li>role 클레임 부재(Phase 1 이전 토큰) → 통과</li>
 *   <li>role 불일치 → 403 FORBIDDEN(T6)</li>
 * </ul>
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
        if (bearerToken == null) {
            // Phase 3a: 무토큰 레거시 요청 허용 — Phase 3b에서 REQUIRED_BEARER_TOKEN(401)로 전환
            return true;
        }

        String token = bearerTokenExtractor.extract(bearerToken);
        Role role = tokenService.extractRole(token);
        if (role == null) {
            // Phase 1 이전에 발급된 토큰은 role 클레임이 없다 — 재로그인 전까지 허용
            return true;
        }

        boolean hasRole = Arrays.asList(requireRole.value()).contains(role);
        if (!hasRole) {
            throw new ApplicationException(TokenExceptionCode.FORBIDDEN);
        }

        return true;
    }
}
