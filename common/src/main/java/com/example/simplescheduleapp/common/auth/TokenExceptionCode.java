package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum TokenExceptionCode implements ExceptionCode {

    REQUIRED_TOKEN(ErrorKind.UNAUTHORIZED, "T0", "토큰 정보가 없습니다."),
    EXPIRED_TOKEN(ErrorKind.UNAUTHORIZED, "T1", "만료된 토큰입니다."),
    INVALID_TOKEN(ErrorKind.UNAUTHORIZED, "T2", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(ErrorKind.UNAUTHORIZED, "T3", "인증되지 않았습니다."),
    REQUIRED_BEARER_TOKEN(ErrorKind.UNAUTHORIZED, "T4", "Bearer 토큰 정보가 없습니다."),
    UNKNOWN_TOKEN(ErrorKind.INTERNAL_SERVER_ERROR, "T5", "예기치 못한 토큰 예외가 발생했습니다."),
    FORBIDDEN(ErrorKind.FORBIDDEN, "T6", "해당 리소스에 대한 접근 권한이 없습니다."),
    // T7(IDENTITY_REQUIRED)은 듀얼리드 전용이었다 — 토큰 필수화(ADR-0005)로 도달 불가능해져 제거했다.
    // 무토큰 요청은 이제 T4(REQUIRED_BEARER_TOKEN)로 응답한다. 코드값 T7은 재사용하지 않는다.
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    TokenExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
