package com.example.simplescheduleapp.schedule.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum ScheduleExceptionCode implements ExceptionCode {

    TUTOR_SCHEDULE_CONFLICT(ErrorKind.CONFLICT, "SC0", "해당 시간에 이미 다른 일정이 존재합니다."),
    STUDENT_SCHEDULE_CONFLICT(ErrorKind.CONFLICT, "SC1", "해당 시간에 이미 수강 중인 강의가 존재합니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    ScheduleExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
