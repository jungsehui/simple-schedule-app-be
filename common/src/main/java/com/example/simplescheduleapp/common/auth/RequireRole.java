package com.example.simplescheduleapp.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 엔드포인트 역할 제한. {@link RoleInterceptor}가 검사한다.
 * <p>
 * Phase 3a(듀얼리드) 동안은 토큰을 제시한 요청에만 적용된다 —
 * 무토큰 레거시 요청은 통과한다. Phase 3b에서 강제 전환 예정.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    Role[] value();
}
