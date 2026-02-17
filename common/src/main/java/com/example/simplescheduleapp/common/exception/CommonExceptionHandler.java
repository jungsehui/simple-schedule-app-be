package com.example.simplescheduleapp.common.exception;

import com.example.simplescheduleapp.common.exception.response.ExceptionResponse;
import com.example.simplescheduleapp.common.exception.response.MethodArgumentExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(value = ApplicationException.class)
    public ResponseEntity<ExceptionResponse> handleApplicationException(ApplicationException exception) {
        ExceptionCode code = exception.getCode();
        log.error("ApplicationException occurred !! code: {} message: {}", code.getCode(), code.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ExceptionResponse.from(code));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<MethodArgumentExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.error("MethodArgumentNotValidException occurred: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(MethodArgumentExceptionResponse.from(InternalServerExceptionCode.INVALID_INPUT_VALUE, errors));
    }
}
