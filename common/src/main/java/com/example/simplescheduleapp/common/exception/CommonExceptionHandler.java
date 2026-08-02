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
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * 존재하지 않는 경로 → 404. 클라이언트 귀책이므로 500이 아니다.
     *
     * <p>이게 없으면 아래 {@code handleException}이 삼켜 <b>500 + 스택트레이스</b>가 나간다.
     * 실제로 SSE 계약 변경 후 구 경로({@code /sse-stream/{memberId}})를 호출한 클라이언트가
     * 404 대신 500을 받았고, 서버 로그에는 매 요청마다 ERROR 스택이 쌓였다.
     */
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("No handler for request. path: {}", exception.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ExceptionResponse.from(InternalServerExceptionCode.RESOURCE_NOT_FOUND));
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
