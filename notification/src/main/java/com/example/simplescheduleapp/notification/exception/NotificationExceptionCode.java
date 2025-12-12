package com.example.simplescheduleapp.notification.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationExceptionCode implements ExceptionCode {

    NOTIFICATION_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "NT0", "지원하지 않는 수강 이벤트입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    NotificationExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
