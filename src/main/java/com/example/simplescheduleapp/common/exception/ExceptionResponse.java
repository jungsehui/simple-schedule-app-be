package com.example.simplescheduleappback.common.exception;

public record ExceptionResponse(
        String code,
        String message
) {

    public static ExceptionResponse from(ExceptionCode code) {
        return new ExceptionResponse(code.getCode(), code.getMessage());
    }
}
