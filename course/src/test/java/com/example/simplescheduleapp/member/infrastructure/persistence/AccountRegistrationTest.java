package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 가입하면 같은 id의 account가 함께 생기고, account 쪽 충돌은 가입 전체를 되돌린다 (ADR-0006 P1).
 */
@DisplayName("가입 시 account 등록 은(는)")
class AccountRegistrationTest extends ApplicationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long signUp(String username, String phone) {
        return studentService.signUpStudent(
                new StudentSignUpCommand(username, "Password1!", "정세희", 20, phone, "학교"));
    }

    @DisplayName("회원 id를 그대로 쓰는 ACTIVE account를 정규화된 username으로 만든다")
    @Test
    void 가입하면_같은_id의_account가_생긴다() {
        Long memberId = signUp("AcctReg1", "01022220001");

        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT username, status, password_hash, external_uuid FROM account WHERE id = ?", memberId);
        assertThat(account.get("username")).isEqualTo("acctreg1");
        assertThat(account.get("status")).isEqualTo("ACTIVE");
        assertThat(account.get("password_hash")).isNull();
        assertThat(account.get("external_uuid")).isNull();
    }

    @DisplayName("GeekChat 출신 account가 같은 username을 쓰고 있으면 중복으로 거절하고 회원도 남기지 않는다")
    @Test
    void account_username이_겹치면_가입_전체가_롤백된다() {
        jdbcTemplate.update(
                "INSERT INTO account (id, external_uuid, username, status, created_at, updated_at) "
                        + "VALUES (900000001, '00000000-0000-0000-0000-000000000001', 'acctreg2', 'ACTIVE', "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        assertThatThrownBy(() -> signUp("acctreg2", "01022220002"))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        assertThat(memberRepository.findByUsername("acctreg2")).isEmpty();
    }
}
