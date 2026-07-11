package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 3a 듀얼리드 식별자 결정 규칙: 토큰 우선, 레거시 파라미터 폴백, 둘 다 없으면 401.
 */
class AuthIdentitiesTest {

    @Test
    void 토큰_식별자가_레거시_파라미터를_이긴다() {
        assertThat(AuthIdentities.resolve(1L, 2L)).isEqualTo(1L);
    }

    @Test
    void 토큰이_없으면_레거시_파라미터로_폴백한다() {
        assertThat(AuthIdentities.resolve(null, 2L)).isEqualTo(2L);
    }

    @Test
    void 둘_다_없으면_IDENTITY_REQUIRED_예외() {
        assertThatThrownBy(() -> AuthIdentities.resolve(null, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", TokenExceptionCode.IDENTITY_REQUIRED);
    }
}
