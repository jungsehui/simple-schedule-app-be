package com.example.simplescheduleapp.common.auth;

/**
 * 회원 역할. {@code Member}의 JOINED 상속 discriminator(STUDENT/TUTOR/PARENT)와 1:1 대응한다.
 * 토큰 claim 및 {@code LoginResponse}에 실려 클라이언트가 서버 확정 역할을 사용하게 한다.
 */
public enum Role {
    STUDENT,
    TUTOR,
    PARENT
}
