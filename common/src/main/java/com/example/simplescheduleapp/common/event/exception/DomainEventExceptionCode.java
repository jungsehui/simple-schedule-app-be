package com.example.simplescheduleapp.common.event.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DomainEventExceptionCode implements ExceptionCode {

    DOMAIN_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DE1", "해당 이벤트를 찾을 수 없습니다."),
    DOMAIN_EVENT_NOT_SUPPORTED(HttpStatus.NOT_FOUND, "DE2", "지원하지 않는 도메인 이벤트입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    DomainEventExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
