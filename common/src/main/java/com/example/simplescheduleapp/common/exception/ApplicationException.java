package com.example.simplescheduleapp.common.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

    private final ExceptionCode code;

    public ApplicationException(ExceptionCode code) {
        super("Exception: %s, Code: %s, Message: %s".formatted(
                code,
                code.getCode(),
                code.getMessage()
        ));
        this.code = code;
    }
}
