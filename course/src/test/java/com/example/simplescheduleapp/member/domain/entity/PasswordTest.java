package com.example.simplescheduleapp.member.domain.entity;

import com.example.simplescheduleapp.member.domain.Password;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordTest {

    @Test
    void 비밀번호_해시_성공() {
        // given
        String plain = "ASDFasdf1234!@#$";

        // when
        Password password = Password.hashPassword(plain);

        // then
        assertThat(password.getHashedPassword()).isNotEqualTo(plain);
    }

    @Test
    void 비밀번호_일치_성공() {
        // given
        String plain = "ASDFasdf1234!@#$";
        Password password = Password.hashPassword(plain);

        // when
        boolean success = password.match(plain);
        boolean fail = password.match(plain + "1");

        // then
        assertThat(success).isEqualTo(true);
        assertThat(fail).isEqualTo(false);
    }
}