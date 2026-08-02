package com.example.simplescheduleapp.notification.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum NotificationTypeExceptionCode implements ExceptionCode {

    NOTIFICATION_TYPE_NOT_FOUND(ErrorKind.NOT_FOUND, "NT0", "지원하지 않는 수강 이벤트입니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    NotificationTypeExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
