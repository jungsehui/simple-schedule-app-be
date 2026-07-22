package com.example.simplescheduleapp.lecture.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum SpecialLectureEnrollmentExceptionCode implements ExceptionCode {

    SPECIAL_LECTURE_ENROLLMENT_FAILED(ErrorKind.INTERNAL_SERVER_ERROR, "SLE001", "수강신청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    SPECIAL_LECTURE_NOT_FOUND(ErrorKind.NOT_FOUND, "SLE002", "해당 특강을 찾을 수 없습니다."),
    ALREADY_ENROLLED(ErrorKind.CONFLICT, "SLE003", "이미 수강신청이 완료된 특강입니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    SpecialLectureEnrollmentExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
