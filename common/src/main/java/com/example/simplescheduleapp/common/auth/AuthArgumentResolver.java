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

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Auth.class)
                && parameter.getParameterType().equals(Long.class);
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
            return tokenService.extractMemberId(token);
        } catch (ApplicationException e) {
            if (!required) {
                return null; // optional-auth: 유효하지 않은 토큰 → null 주입
            }
            throw e;
        }
    }

    private boolean isRequired(MethodParameter parameter) {
        Auth auth = parameter.getParameterAnnotation(Auth.class);
        return auth == null || auth.required();
    }
}
