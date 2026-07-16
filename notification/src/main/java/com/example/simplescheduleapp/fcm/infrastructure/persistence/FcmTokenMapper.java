package com.example.simplescheduleapp.fcm.infrastructure.persistence;

import com.example.simplescheduleapp.fcm.domain.FcmToken;

/**
 * 순수 도메인 {@code FcmToken} ↔ JPA {@code FcmTokenEntity} 변환 (ADR-0004).
 */
final class FcmTokenMapper {

    private FcmTokenMapper() {
    }

    static FcmToken toDomain(FcmTokenEntity entity) {
        return FcmToken.reconstitute(entity.getId(), entity.getMemberId(), entity.getFcmToken());
    }

    static FcmTokenEntity toEntity(FcmToken domain) {
        return new FcmTokenEntity(domain.getId(), domain.getMemberId(), domain.getFcmToken());
    }
}
