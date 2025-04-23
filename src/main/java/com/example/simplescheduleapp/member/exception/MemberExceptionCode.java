package com.example.simplescheduleapp.member.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberExceptionCode implements ExceptionCode {

    INVALID_USERNAME_PASSWORD(HttpStatus.UNAUTHORIZED, "M0", "잘못된 아이디 혹은 비밀번호입니다."),
    DUPLICATED_USERNAME_PHONE(HttpStatus.CONFLICT, "M1", "아이디 혹은 휴대폰 번호가 중복되었습니다. 다른 아이디 혹은 휴대폰 번호를 사용해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    MemberExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
