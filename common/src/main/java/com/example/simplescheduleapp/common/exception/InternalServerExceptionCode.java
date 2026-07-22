package com.example.simplescheduleapp.common.exception;

import lombok.Getter;

@Getter
public enum InternalServerExceptionCode implements ExceptionCode {

    UNKNOWN_EXCEPTION(ErrorKind.INTERNAL_SERVER_ERROR, "ISE1", "알 수 없는 예외가 발생했습니다."),
    EXTERNAL_API_ERROR(ErrorKind.INTERNAL_SERVER_ERROR, "ISE2", "API 호출 중 예외가 발생했습니다."),
    INVALID_INPUT_VALUE(ErrorKind.BAD_REQUEST, "ISE3", "유효하지 않은 입력값입니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    InternalServerExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
