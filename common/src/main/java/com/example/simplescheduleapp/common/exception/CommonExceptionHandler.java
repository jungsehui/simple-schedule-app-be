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
        HttpStatus status = toHttpStatus(code.getKind());
        if (status.is5xxServerError()) {
            log.error("ApplicationException occurred. code: {}, message: {}", code.getCode(), code.getMessage(), exception);
        } else {
            log.warn("ApplicationException occurred. code: {}, message: {}", code.getCode(), code.getMessage());
        }
        return ResponseEntity
                .status(status)
                .body(ExceptionResponse.from(code));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<MethodArgumentExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("MethodArgumentNotValidException occurred: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(MethodArgumentExceptionResponse.from(InternalServerExceptionCode.INVALID_INPUT_VALUE, errors));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception exception) {
        log.error("Unhandled exception occurred.", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.from(InternalServerExceptionCode.UNKNOWN_EXCEPTION));
    }

    /**
     * {@link ErrorKind} → HTTP 상태 변환 — 이 매핑은 웹 어댑터인 여기에만 존재한다.
     * switch는 망라형(exhaustive)이라 새 ErrorKind 추가 시 컴파일 에러로 매핑 누락을 잡는다.
     */
    private static HttpStatus toHttpStatus(ErrorKind kind) {
        return switch (kind) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
