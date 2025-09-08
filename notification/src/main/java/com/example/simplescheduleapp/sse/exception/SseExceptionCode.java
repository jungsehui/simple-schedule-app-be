package com.example.simplescheduleapp.sse.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SseExceptionCode implements ExceptionCode {

    SSE_NOT_FOUND(HttpStatus.NOT_FOUND, "S0", "해당 SSE 이벤트가 없습니다."),
    SSE_SEND_FAILED(HttpStatus.CONFLICT, "S1", "SSE 알림 전송에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SseExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
