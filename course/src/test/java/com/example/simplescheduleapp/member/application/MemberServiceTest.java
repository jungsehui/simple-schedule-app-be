package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
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
        mockMember = new Member("shinddonghun", "Password123!", "신동훈", 25, "01023423452");
    }

    @Test
    void 아이디_입력_실패() {
        // given
        when(memberRepository.getByUsername("nonexistent"));

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("nonexistent", "Password123!"));
    }

    @Test
    void 비밀번호_불일치_실패() {
        // given
        when(memberRepository.getByUsername("shinddonghun"));

        // when & then
        assertThrows(ApplicationException.class, () -> memberService.login("shinddonghun", "WrongPassword"));
    }
}
