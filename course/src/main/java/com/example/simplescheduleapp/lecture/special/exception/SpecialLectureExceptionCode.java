package com.example.simplescheduleapp.lecture.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum SpecialLectureExceptionCode implements ExceptionCode {

    SPECIAL_LECTURE_NOT_FOUND(ErrorKind.NOT_FOUND, "SL001", "특별 강의를 찾을 수 없습니다."),
    /**
     * 정원 정보를 확인할 수 없다. <b>메시지에 내부 인프라명을 쓰지 않는다</b> — 사용자 화면에
     * "Redis"가 나가고 있었다. 상수명도 같은 이유로 바꿨다(로그·스택트레이스에도 드러난다).
     * 와이어 코드 {@code SL002}는 그대로라 클라이언트 계약은 바뀌지 않는다.
     */
    SPECIAL_LECTURE_CAPACITY_UNAVAILABLE(ErrorKind.NOT_FOUND, "SL002", "특강 정원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."),
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
