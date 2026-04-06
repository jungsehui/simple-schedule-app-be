package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

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

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null) {
            throw new ApplicationException(TokenExceptionCode.REQUIRED_BEARER_TOKEN);
        }

        String token = bearerTokenExtractor.extract(bearerToken);
        MemberRole memberRole = tokenService.extractRole(token);
        if (memberRole == null) {
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        }

        MemberRole[] allowedRoles = requireRole.value();
        boolean hasRole = Arrays.asList(allowedRoles).contains(memberRole);
        if (!hasRole) {
            throw new ApplicationException(TokenExceptionCode.FORBIDDEN);
        }

        return true;
    }
}
