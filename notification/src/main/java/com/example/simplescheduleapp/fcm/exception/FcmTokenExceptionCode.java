package com.example.simplescheduleapp.fcm.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FcmTokenExceptionCode implements ExceptionCode {

    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FT0", "해당하는 FCM 토큰 정보가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    FcmTokenExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
