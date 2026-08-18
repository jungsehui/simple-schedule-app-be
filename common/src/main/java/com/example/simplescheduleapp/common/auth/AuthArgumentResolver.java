package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
@Component
public class AuthArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenService tokenService;
    private final BearerTokenExtractor bearerTokenExtractor;

    /**
     * {@code @Auth}는 "이 값은 토큰에서 온다"는 뜻이고, <b>어떤 값인지는 파라미터 타입이 정한다</b> —
     * {@code Long}이면 회원 식별자, {@link Role}이면 역할이다. 애노테이션을 종류별로 늘리지 않는 이유는
     * 규칙이 하나여야 새 엔드포인트가 실수 없이 따라오기 때문이다.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(Auth.class)) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return type.equals(Long.class) || type.equals(Role.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        boolean required = isRequired(parameter);
        HttpServletRequest httpServletRequest = (HttpServletRequest) webRequest.getNativeRequest();

        String bearerToken = httpServletRequest.getHeader("Authorization");
        if (bearerToken == null) {
            if (!required) {
                return null; // optional-auth: 토큰 없음 → null 주입 (엔드포인트가 파라미터로 폴백)
            }
            throw new ApplicationException(TokenExceptionCode.REQUIRED_BEARER_TOKEN);
        }

        try {
            String token = bearerTokenExtractor.extract(bearerToken);
            if (parameter.getParameterType().equals(Role.class)) {
                return resolveRole(token, required);
            }
            return tokenService.extractMemberId(token);
        } catch (ApplicationException e) {
            if (!required) {
                return null; // optional-auth: 유효하지 않은 토큰 → null 주입
            }
            throw e;
        }
    }

    /**
     * 역할 클레임이 없는 토큰은 거부한다.
     *
     * <p>{@code RoleInterceptor}는 {@code @RequireRole}이 붙은 곳에서만 이 검사를 하므로,
     * 역할 제한 없이 역할 <em>값</em>만 쓰는 엔드포인트({@code GET /me/schedules})는 그 그물에
     * 걸리지 않는다. 여기서 막지 않으면 null 역할이 유스케이스까지 내려가 NPE가 된다.
     * 판단 근거가 없는 토큰을 통과시키는 것은 fail-open이므로 인터셉터와 같은 403으로 맞춘다.
     */
    private Role resolveRole(String token, boolean required) {
        Role role = tokenService.extractRole(token);
        if (role == null && required) {
            throw new ApplicationException(TokenExceptionCode.FORBIDDEN);
        }
        return role;
    }

    private boolean isRequired(MethodParameter parameter) {
        Auth auth = parameter.getParameterAnnotation(Auth.class);
        return auth == null || auth.required();
    }
}
