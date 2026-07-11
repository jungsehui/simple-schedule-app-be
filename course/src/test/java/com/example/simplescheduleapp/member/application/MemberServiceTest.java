package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.student.domain.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    private Member mockMember;

    @BeforeEach
    void setUp() {
        // Member는 추상 타입이므로 구체 서브타입(Student)으로 생성 — login/password 동작은 동일하게 상속
        mockMember = new Student("jungsehui", "Password123!", "정세희", 25, "01023423452", "테스트고등학교");
    }

    @Test
    void 아이디_입력_실패() {
        // given: 존재하지 않는 아이디는 실제 getByUsername이 예외를 던지는 동작을 재현
        when(memberRepository.getByUsername("nonexistent"))
                .thenThrow(new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD));

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("nonexistent", "Password123!"));
    }

    @Test
    void 비밀번호_불일치_실패() {
        // given: 아이디는 존재하지만(회원 반환) 비밀번호가 달라 로그인 시 예외가 발생
        when(memberRepository.getByUsername("jungsehui")).thenReturn(mockMember);

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("jungsehui", "WrongPassword"));
    }
}
