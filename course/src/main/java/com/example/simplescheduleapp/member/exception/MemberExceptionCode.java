package com.example.simplescheduleapp.member.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum MemberExceptionCode implements ExceptionCode {

    INVALID_USERNAME_PASSWORD(ErrorKind.UNAUTHORIZED, "M0", "잘못된 아이디 혹은 비밀번호입니다."),
    DUPLICATED_USERNAME_PHONE(ErrorKind.CONFLICT, "M1", "아이디 혹은 휴대폰 번호가 중복되었습니다. 다른 아이디 혹은 휴대폰 번호를 사용해주세요."),
    MEMBER_NOT_FOUND(ErrorKind.NOT_FOUND, "M2", "해당 id를 가진 회원이 없습니다."),
    TUTOR_NOT_FOUND(ErrorKind.NOT_FOUND, "T0", "해당 id를 가진 강사가 없습니다."),
    STUDENT_NOT_FOUND(ErrorKind.NOT_FOUND, "S0", "해당 id를 가진 학생이 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    MemberExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
