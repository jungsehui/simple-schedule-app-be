package com.example.simplescheduleapp.notification.domain.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FailedNotificationExceptionCode implements ExceptionCode {

    FAILED_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "FN0", "실패한 알림 정보가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    FailedNotificationExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
