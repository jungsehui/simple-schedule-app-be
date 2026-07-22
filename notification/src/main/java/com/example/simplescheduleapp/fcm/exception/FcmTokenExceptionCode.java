package com.example.simplescheduleapp.fcm.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum FcmTokenExceptionCode implements ExceptionCode {

    FCM_TOKEN_NOT_FOUND(ErrorKind.NOT_FOUND, "FT0", "해당하는 FCM 토큰 정보가 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    FcmTokenExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
