package com.example.simplescheduleapp.lecture.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PendingLectureEnrollmentExceptionCode implements ExceptionCode {

    ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "PLE0", "이미 수강신청 취소한 내용입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    PendingLectureEnrollmentExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
