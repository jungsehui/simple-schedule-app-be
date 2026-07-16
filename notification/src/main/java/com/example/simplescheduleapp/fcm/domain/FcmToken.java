package com.example.simplescheduleapp.fcm.domain;

import lombok.Getter;

/**
 * FCM 디바이스 토큰 — 순수 도메인 모델 (ADR-0004).
 * <p>JPA/프레임워크 의존 0. 영속 매핑은 {@code infrastructure/persistence}의
 * {@code FcmTokenEntity}가 담당한다.
 */
@Getter
public class FcmToken {

    private final Long id;
    private final Long memberId;
    private final String fcmToken;

    public FcmToken(Long memberId, String fcmToken) {
        this(null, memberId, fcmToken);
    }

    private FcmToken(Long id, Long memberId, String fcmToken) {
        this.id = id;
        this.memberId = memberId;
        this.fcmToken = fcmToken;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static FcmToken reconstitute(Long id, Long memberId, String fcmToken) {
        return new FcmToken(id, memberId, fcmToken);
    }
}
