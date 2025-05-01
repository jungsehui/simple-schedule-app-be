package com.example.simplescheduleapp.lecture.domain.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LectureExceptionCode implements ExceptionCode {

    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "L0", "해당 강의가 없습니다."),
    INVALID_LECTURE_TIME_PAST(HttpStatus.BAD_REQUEST, "L1", "종료 시간이 시작 시간보다 이전일 수 없습니다."),
    TUTOR_UNAUTHORIZED(HttpStatus.BAD_REQUEST, "L2", "강의에 대한 권한이 없습니다."),
    ALREADY_ENROLLED(HttpStatus.BAD_REQUEST, "L3", "이미 수강 신청한 강의입니다."),
    CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "L4", "수강 정원이 초과되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    LectureExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
