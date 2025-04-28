package com.example.simplescheduleapp.tutor.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TutorExceptionCode implements ExceptionCode {

    TUTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "M0", "해당 id를 가진 강사가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TutorExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
