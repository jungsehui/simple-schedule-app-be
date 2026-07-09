package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TokenExceptionCode implements ExceptionCode {

    REQUIRED_TOKEN(HttpStatus.UNAUTHORIZED, "T0", "토큰 정보가 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "T1", "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "T2", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "T3", "인증되지 않았습니다."),
    REQUIRED_BEARER_TOKEN(HttpStatus.UNAUTHORIZED, "T4", "Bearer 토큰 정보가 없습니다."),
    UNKNOWN_TOKEN(HttpStatus.INTERNAL_SERVER_ERROR, "T5", "예기치 못한 토큰 예외가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TokenExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
