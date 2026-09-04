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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// 순수 단위 테스트(@Mock/@InjectMocks) — Spring 컨텍스트 불필요. MockitoExtension이 @Mock을 처리한다.
// (SpringExtension은 @Mock을 주입하지 않아 NPE — Boot 4의 Spring Test에서 표면화)
@ExtendWith(MockitoExtension.class)
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
        // given: 조회가 비어 있는 상태. 로그인은 정확 일치와 정규화 두 형태를 모두 찾아보고
        // 그래도 없으면 INVALID_USERNAME_PASSWORD를 던진다. "nonexistent"는 이미 정규형이라
        // 두 번째 질의는 건너뛰므로 스텁도 하나면 된다.
        when(memberRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("nonexistent", "Password123!"));
    }

    @Test
    void 비밀번호_불일치_실패() {
        // given: 아이디는 존재하지만(회원 반환) 비밀번호가 달라 로그인 시 예외가 발생
        when(memberRepository.findByUsername("jungsehui")).thenReturn(Optional.of(mockMember));

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("jungsehui", "WrongPassword"));
    }
}
