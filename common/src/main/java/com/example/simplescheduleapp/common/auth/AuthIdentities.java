package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;

/**
 * 인증 마이그레이션 Phase 3a 듀얼리드 식별자 결정 규칙: 토큰이 이기고, 레거시 파라미터가 폴백.
 */
public final class AuthIdentities {

    private AuthIdentities() {
    }

    /**
     * @param tokenMemberId {@code @Auth(required = false)}로 주입된 토큰 식별자 (없으면 null)
     * @param legacyParamId 레거시 요청 파라미터 식별자 (없으면 null)
     * @return 결정된 회원 식별자
     * @throws ApplicationException 둘 다 없으면 IDENTITY_REQUIRED(T7, 401)
     */
    public static Long resolve(Long tokenMemberId, Long legacyParamId) {
        if (tokenMemberId != null) {
            return tokenMemberId;
        }
        if (legacyParamId != null) {
            return legacyParamId;
        }
        throw new ApplicationException(TokenExceptionCode.IDENTITY_REQUIRED);
    }
}
