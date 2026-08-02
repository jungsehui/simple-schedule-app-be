package com.example.simplescheduleapp.common.event.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum DomainEventExceptionCode implements ExceptionCode {

    DOMAIN_EVENT_NOT_FOUND(ErrorKind.NOT_FOUND, "DE1", "해당 이벤트를 찾을 수 없습니다."),
    DOMAIN_EVENT_NOT_SUPPORTED(ErrorKind.NOT_FOUND, "DE2", "지원하지 않는 도메인 이벤트입니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    DomainEventExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
