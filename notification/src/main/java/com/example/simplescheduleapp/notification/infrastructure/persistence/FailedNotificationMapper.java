package com.example.simplescheduleapp.notification.infrastructure.persistence;

import com.example.simplescheduleapp.notification.domain.FailedNotification;

/**
 * 순수 도메인 {@code FailedNotification} ↔ JPA {@code FailedNotificationEntity} 변환 (ADR-0004).
 */
final class FailedNotificationMapper {

    private FailedNotificationMapper() {
    }

    static FailedNotification toDomain(FailedNotificationEntity entity) {
        return FailedNotification.reconstitute(
                entity.getId(),
                entity.getUuid(),
                entity.getSenderId(),
                entity.getTargetId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getType(),
                entity.getFailReason(),
                entity.getRetryCount()
        );
    }

    static FailedNotificationEntity toEntity(FailedNotification domain) {
        return new FailedNotificationEntity(
                domain.getId(),
                domain.getUuid(),
                domain.getSenderId(),
                domain.getTargetId(),
                domain.getTitle(),
                domain.getBody(),
                domain.getType(),
                domain.getFailReason(),
                domain.getRetryCount()
        );
    }
}
