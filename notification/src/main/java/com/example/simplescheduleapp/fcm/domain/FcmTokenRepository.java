package com.example.simplescheduleapp.fcm.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;

import java.util.Optional;

/**
 * {@code FcmToken} 영속 포트 — 순수 도메인 인터페이스 (ADR-0004).
 * <p>Spring Data/JPA 세부는 {@code infrastructure/persistence}의 어댑터에 격리된다.
 */
public interface FcmTokenRepository {

    FcmToken save(FcmToken fcmToken);

    Optional<FcmToken> findByMemberId(Long memberId);

    default FcmToken getByMemberId(Long memberId) {
        return findByMemberId(memberId)
                .orElseThrow(() -> new ApplicationException(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND));
    }
}
