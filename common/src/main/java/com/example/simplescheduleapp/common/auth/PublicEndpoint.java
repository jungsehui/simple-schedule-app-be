package com.example.simplescheduleapp.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 토큰 없이 호출할 수 있는 엔드포인트 표시 (ADR-0005).
 *
 * <p>인증은 {@link AuthenticationInterceptor}가 <b>기본 차단</b>으로 강제한다. 즉 이 애노테이션이
 * 없는 모든 엔드포인트는 토큰을 요구한다 — 새 API를 추가할 때 보호를 <em>기억해서 붙이는</em> 것이
 * 아니라, 공개해야 할 때만 <em>명시적으로 여는</em> 구조다.
 *
 * <p>붙일 수 있는 곳은 로그인·회원가입처럼 토큰을 아직 가질 수 없는 요청뿐이다. 그 외에 이 애노테이션을
 * 다는 것은 인가 구멍을 뚫는 일이므로, 반드시 이유를 주석으로 남긴다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
