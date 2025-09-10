package com.example.simplescheduleapp.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SpecialLectureExceptionEnrollmentCode implements ExceptionCode {

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SpecialLectureExceptionEnrollmentCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
