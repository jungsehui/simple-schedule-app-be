package com.example.simplescheduleapp.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InternalServerExceptionCode implements ExceptionCode {

    UNKNOWN_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "ISE1", "알 수 없는 예외가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ISE2", "API 호출 중 예외가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "ISE3", "유효하지 않은 입력값입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InternalServerExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
