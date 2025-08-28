package com.example.simplescheduleapp.fcm.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FcmExceptionCode implements ExceptionCode {

    FCM_SEND_FAILED(HttpStatus.CONFLICT, "F0", "FCM 전송에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    FcmExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
