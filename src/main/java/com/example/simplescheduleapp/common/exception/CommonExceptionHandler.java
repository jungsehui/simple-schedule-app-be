package com.example.simplescheduleapp.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(value = ApplicationException.class)
    public ResponseEntity<ExceptionResponse> handleApplicationException(ApplicationException exception) {
        ExceptionCode code = exception.getCode();
        log.info("ApplicationException occurred !! code: {} message: {}", code.getCode(), code.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ExceptionResponse.from(code));
    }
}
