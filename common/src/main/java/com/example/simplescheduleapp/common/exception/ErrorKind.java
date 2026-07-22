package com.example.simplescheduleapp.common.exception;

/**
 * 예외의 의미 분류 — 전송 기술 중립 (ADR-0004 후속).
 *
 * <p>예외 코드는 "무엇이 잘못됐는가"(없음/충돌/권한 없음…)만 말하고, 그것이 HTTP 상태 코드로
 * 어떻게 표현되는지는 웹 어댑터({@link CommonExceptionHandler})가 결정한다. 덕분에 도메인·공유
 * 커널의 예외 코드가 Spring Web에 의존하지 않는다.
 *
 * <p>이름은 보편적 오류 분류를 따르며 의도적으로 HTTP 상태명과 1:1이다 — 매핑 실수 여지를
 * 없애고, 새 분류가 필요해지기 전까지는 추측성 추상화를 더하지 않는다.
 */
public enum ErrorKind {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_SERVER_ERROR,
}
