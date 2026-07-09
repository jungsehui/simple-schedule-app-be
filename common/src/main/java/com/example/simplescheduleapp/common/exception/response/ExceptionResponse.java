package com.example.simplescheduleapp.common.exception.response;

import com.example.simplescheduleapp.common.exception.ExceptionCode;

public record ExceptionResponse(
        String code,
        String message
) {

    public static ExceptionResponse from(ExceptionCode code) {
        return new ExceptionResponse(code.getCode(), code.getMessage());
    }
}
