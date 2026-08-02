package com.example.simplescheduleapp.common.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    /** {@code InternalApiKeyFilter}(공유 시크릿)가 가드하는 서버 간 경로 — 회원 토큰 대상이 아니다. */
    private static final String INTERNAL_PATH_PATTERN = "/internal/**";

    private final AuthArgumentResolver authArgumentResolver;
    private final AuthenticationInterceptor authenticationInterceptor;
    private final RoleInterceptor roleInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 순서가 계약이다: 인증(누구인가)이 통과한 뒤에만 인가(무엇을 할 수 있는가)를 따진다.
        // 그래야 RoleInterceptor가 "토큰은 이미 유효하다"를 전제로 역할만 판단할 수 있다. (ADR-0005)
        registry.addInterceptor(authenticationInterceptor)
                .excludePathPatterns(INTERNAL_PATH_PATTERN);
        registry.addInterceptor(roleInterceptor)
                .excludePathPatterns(INTERNAL_PATH_PATTERN);
    }
}
