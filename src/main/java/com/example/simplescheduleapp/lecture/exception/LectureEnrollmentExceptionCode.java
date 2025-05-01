package com.example.simplescheduleapp.lecture.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LectureEnrollmentExceptionCode implements ExceptionCode {

    LECTURE_ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "LE0", "강의 등록 정보가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    LectureEnrollmentExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
