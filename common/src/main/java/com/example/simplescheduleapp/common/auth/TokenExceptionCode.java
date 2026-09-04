package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum TokenExceptionCode implements ExceptionCode {

    REQUIRED_TOKEN(ErrorKind.UNAUTHORIZED, "T0", "토큰 정보가 없습니다."),
    EXPIRED_TOKEN(ErrorKind.UNAUTHORIZED, "T1", "만료된 토큰입니다."),
    INVALID_TOKEN(ErrorKind.UNAUTHORIZED, "T2", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(ErrorKind.UNAUTHORIZED, "T3", "인증되지 않았습니다."),
    REQUIRED_BEARER_TOKEN(ErrorKind.UNAUTHORIZED, "T4", "Bearer 토큰 정보가 없습니다."),
    UNKNOWN_TOKEN(ErrorKind.INTERNAL_SERVER_ERROR, "T5", "예기치 못한 토큰 예외가 발생했습니다."),
    FORBIDDEN(ErrorKind.FORBIDDEN, "T6", "해당 리소스에 대한 접근 권한이 없습니다."),
    REQUIRED_ROLE_CLAIM(ErrorKind.FORBIDDEN, "T8", "역할 정보가 없는 토큰입니다. 다시 로그인해 주세요."),
    // T7(IDENTITY_REQUIRED)은 듀얼리드 전용이었다 — 토큰 필수화(ADR-0005)로 도달 불가능해져 제거했다.
    // 무토큰 요청은 이제 T4(REQUIRED_BEARER_TOKEN)로 응답한다. 코드값 T7은 재사용하지 않는다.
    //
    // T6과 T8은 둘 다 403이지만 원인이 다르고, 클라이언트가 취해야 할 행동도 다르다.
    //   T8(역할 클레임 부재) — 판단 근거가 없는 토큰이다. 재인증이 맞다.
    //   T6(역할 불일치)     — 세션은 멀쩡하고 이 리소스에 권한이 없을 뿐이다. 세션을 정리하면 안 된다.
    // 이전에는 둘 다 T6이라 클라이언트가 구분할 수 없었고, 그래서 "403이면 재인증"으로 처리하면
    // 정상 로그인한 TUTOR가 STUDENT 전용 엔드포인트를 호출하는 순간 강제 로그아웃됐다.
    // 상태코드를 바꾸지 않고 코드만 나눈 이유는 추가형이라 기존 클라이언트가 깨지지 않기 때문이다.
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    TokenExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
