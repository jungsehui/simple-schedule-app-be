package com.example.simplescheduleapp.lecture.general.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LectureExceptionCode implements ExceptionCode {

    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "L0", "해당 강의가 없습니다."),
    INVALID_LECTURE_TIME_PAST(HttpStatus.BAD_REQUEST, "L1", "종료 시간이 시작 시간보다 이전일 수 없습니다."),
    TUTOR_UNAUTHORIZED(HttpStatus.BAD_REQUEST, "L2", "강의에 대한 권한이 없습니다."),
    ALREADY_ENROLLED(HttpStatus.BAD_REQUEST, "L3", "이미 수강 등록된 강의입니다."),
    CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "L4", "수강 정원이 초과되었습니다."),
    ALREADY_REQUESTED(HttpStatus.BAD_REQUEST, "L5", "이미 수강 요청한 강의입니다."),
    CAPACITY_UNDER_ZERO(HttpStatus.BAD_REQUEST, "L6", "수강 인원은 1보다 작을 수 없습니다."),
    CAPACITY_INFO_NOT_FOUND(HttpStatus.BAD_REQUEST, "L7", "수강 인원 정보가 없습니다."),
    CAPACITY_BELOW_ENROLLED(HttpStatus.BAD_REQUEST, "L8", "정원은 현재 수강 인원보다 적을 수 없습니다."),
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
