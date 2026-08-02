package com.example.simplescheduleapp.lecture.general.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum PendingLectureEnrollmentExceptionCode implements ExceptionCode {

    ALREADY_CANCELED(ErrorKind.BAD_REQUEST, "PLE0", "이미 수강신청 취소한 내용입니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    PendingLectureEnrollmentExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
