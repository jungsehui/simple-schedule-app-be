package com.example.simplescheduleapp.notification.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum FailedNotificationExceptionCode implements ExceptionCode {

    FAILED_NOTIFICATION_NOT_FOUND(ErrorKind.NOT_FOUND, "FN0", "실패한 알림 정보가 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    FailedNotificationExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
