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
    IDENTITY_REQUIRED(ErrorKind.UNAUTHORIZED, "T7", "인증 토큰 또는 회원 식별자 파라미터가 필요합니다."),
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
