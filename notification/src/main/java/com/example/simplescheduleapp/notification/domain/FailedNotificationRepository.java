package com.example.simplescheduleapp.notification.domain;

import java.util.List;
import java.util.Optional;

/**
 * {@code FailedNotification} 영속 포트 — 순수 도메인 인터페이스 (ADR-0004).
 * <p>Spring Data/JPA 세부는 {@code infrastructure/persistence}의 어댑터에 격리된다.
 */
public interface FailedNotificationRepository {

    FailedNotification save(FailedNotification failedNotification);

    void delete(FailedNotification failedNotification);

    long count();

    List<FailedNotification> findAll();

    /** 재시도 횟수가 {@code retryCount} 미만인 대상을 최대 {@code limit}건 조회. */
    List<FailedNotification> findByRetryCountLessThan(int retryCount, int limit);

    /** 재시도 처리를 위해 비관적 쓰기 락을 걸고 단건 조회. */
    Optional<FailedNotification> findByIdWithLock(Long id);
}
