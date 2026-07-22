package com.example.simplescheduleapp.lecture.general.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum LectureEnrollmentExceptionCode implements ExceptionCode {

    LECTURE_ENROLLMENT_NOT_FOUND(ErrorKind.NOT_FOUND, "LE0", "강의 등록 정보가 없습니다."),
    PENDING_LECTURE_ENROLLMENT_NOT_FOUND(ErrorKind.NOT_FOUND, "LE1", "대기 중인 수강신청 정보가 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    LectureEnrollmentExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
