package com.example.simplescheduleapp.lecture.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SpecialLectureExceptionCode implements ExceptionCode {

    SPECIAL_LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "SL001", "특별 강의를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SpecialLectureExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
