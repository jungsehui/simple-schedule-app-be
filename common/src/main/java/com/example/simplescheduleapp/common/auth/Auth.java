package com.example.simplescheduleapp.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {

    /**
     * true(기본): 토큰이 없거나 유효하지 않으면 401 예외.
     * false: 토큰이 없거나 유효하지 않으면 {@code null} 주입(optional-auth).
     * 인증 도입 과도기(클라이언트가 아직 토큰을 안 보낼 수 있음)에 dual-read를 위해 사용한다.
     */
    boolean required() default true;
}
