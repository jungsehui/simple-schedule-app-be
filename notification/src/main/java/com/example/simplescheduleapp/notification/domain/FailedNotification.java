package com.example.simplescheduleapp.notification.domain;

import lombok.Getter;

import java.util.UUID;

/**
 * 전송 실패 알림 — 순수 도메인 모델 (ADR-0004).
 * <p>JPA/프레임워크 의존 0. 영속·감사·소프트삭제 관심사는
 * {@code infrastructure/persistence}의 {@code FailedNotificationEntity}가 담당하고,
 * 매퍼가 이 도메인과 변환한다.
 */
@Getter
public class FailedNotification {

    private final Long id;
    private final String uuid;
    private final Long senderId;
    private final Long targetId;
    private final String title;
    private final String body;
    private final NotificationType type;
    private final String failReason;
    private int retryCount;

    /** 신규 실패 알림 생성 — uuid 자동 부여, retryCount=1, id는 영속 시 부여. */
    public FailedNotification(
            Long senderId,
            Long targetId,
            String title,
            String body,
            NotificationType type,
            String failReason
    ) {
        this(null, UUID.randomUUID().toString(), senderId, targetId, title, body, type, failReason, 1);
    }

    private FailedNotification(
            Long id,
            String uuid,
            Long senderId,
            Long targetId,
            String title,
            String body,
            NotificationType type,
            String failReason,
            int retryCount
    ) {
        this.id = id;
        this.uuid = uuid;
        this.senderId = senderId;
        this.targetId = targetId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.failReason = failReason;
        this.retryCount = retryCount;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static FailedNotification reconstitute(
            Long id,
            String uuid,
            Long senderId,
            Long targetId,
            String title,
            String body,
            NotificationType type,
            String failReason,
            int retryCount
    ) {
        return new FailedNotification(id, uuid, senderId, targetId, title, body, type, failReason, retryCount);
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
