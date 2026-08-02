package com.example.simplescheduleapp.notification.infrastructure.persistence;

import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@code FailedNotificationRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>도메인 ↔ 엔티티 매핑을 담당하고, 비관적 락·소프트삭제(@SQLDelete) 시맨틱을 보존한다.
 */
@Repository
@RequiredArgsConstructor
public class FailedNotificationRepositoryAdapter implements FailedNotificationRepository {

    private final FailedNotificationJpaRepository jpaRepository;

    @Override
    public FailedNotification save(FailedNotification failedNotification) {
        FailedNotificationEntity saved = jpaRepository.save(FailedNotificationMapper.toEntity(failedNotification));
        return FailedNotificationMapper.toDomain(saved);
    }

    @Override
    public void delete(FailedNotification failedNotification) {
        // @SQLDelete(소프트삭제) 발동 — deleteById가 로드 후 delete를 호출한다.
        jpaRepository.deleteById(failedNotification.getId());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public List<FailedNotification> findAll() {
        return jpaRepository.findAll().stream()
                .map(FailedNotificationMapper::toDomain)
                .toList();
    }

    @Override
    public List<FailedNotification> findByRetryCountLessThan(int retryCount, int limit) {
        return jpaRepository.findByRetryCountLessThan(retryCount, PageRequest.of(0, limit)).stream()
                .map(FailedNotificationMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FailedNotification> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id).map(FailedNotificationMapper::toDomain);
    }
}
