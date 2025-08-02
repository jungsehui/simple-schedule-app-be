package com.example.simplescheduleapp.member.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberExceptionCode implements ExceptionCode {

    INVALID_USERNAME_PASSWORD(HttpStatus.UNAUTHORIZED, "M0", "잘못된 아이디 혹은 비밀번호입니다."),
    DUPLICATED_USERNAME_PHONE(HttpStatus.CONFLICT, "M1", "아이디 혹은 휴대폰 번호가 중복되었습니다. 다른 아이디 혹은 휴대폰 번호를 사용해주세요."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M2", "해당 id를 가진 회원이 없습니다."),
    TUTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "T0", "해당 id를 가진 강사가 없습니다."),
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S0", "해당 id를 가진 학생이 없습니다."),
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FT0", "해당하는 FCM 토큰 정보가 없습니다."),
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
