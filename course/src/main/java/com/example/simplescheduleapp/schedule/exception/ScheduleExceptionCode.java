package com.example.simplescheduleapp.schedule.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ScheduleExceptionCode implements ExceptionCode {

    TUTOR_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "SC0", "해당 시간에 이미 다른 일정이 존재합니다."),
    STUDENT_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "SC1", "해당 시간에 이미 수강 중인 강의가 존재합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ScheduleExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
