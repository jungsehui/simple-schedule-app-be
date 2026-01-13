package com.example.simplescheduleapp.common.exception.response;

import com.example.simplescheduleapp.common.exception.ExceptionCode;

import java.util.Map;

public record MethodArgumentExceptionResponse(
        String code,
        String message,
        Map<String, String> validationErrors
) {

    public static MethodArgumentExceptionResponse from(ExceptionCode code, Map<String, String> validationErrors) {
        return new MethodArgumentExceptionResponse(
                code.getCode(),
                code.getMessage(),
                validationErrors
        );
    }
}
