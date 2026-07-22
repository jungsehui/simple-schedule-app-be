package com.example.simplescheduleapp.lecture.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum SpecialLectureExceptionCode implements ExceptionCode {

    SPECIAL_LECTURE_NOT_FOUND(ErrorKind.NOT_FOUND, "SL001", "특별 강의를 찾을 수 없습니다."),
    SPECIAL_LECTURE_NOT_FOUND_IN_REDIS(ErrorKind.NOT_FOUND, "SL002", "Redis에서 특별 강의 정원 정보를 찾을 수 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    SpecialLectureExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
