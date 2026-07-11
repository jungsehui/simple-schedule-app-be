package com.example.simplescheduleapp.member.domain.entity;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.student.domain.Student;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberTest {

    private Member member;

    @BeforeEach
    void setUp() {
        // Member는 추상 타입이 되어 구체 서브타입(Student)으로 생성 — login/password 스펙은 동일하게 상속
        member = new Student("jungsehui", "Password123!", "정세희", 28, "01023234545", "테스트고등학교");
    }

    @Test
    void 비밀번호가_일치하면_로그인_성공() {
        Assertions.assertDoesNotThrow(() -> {
            member.login("Password123!");
        });
    }

    @Test
    void 비밀번호가_일치하지_않으면_로그인_실패() {
        assertThrows(ApplicationException.class, () -> member.login("Password1234@"));
    }
}
