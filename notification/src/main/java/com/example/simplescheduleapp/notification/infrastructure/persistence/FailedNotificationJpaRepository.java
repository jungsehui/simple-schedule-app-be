package com.example.simplescheduleapp.notification.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface FailedNotificationJpaRepository extends JpaRepository<FailedNotificationEntity, Long> {

    List<FailedNotificationEntity> findByRetryCountLessThan(int retryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fn from FailedNotificationEntity fn where fn.id = :id")
    Optional<FailedNotificationEntity> findByIdWithLock(@Param("id") Long id);
}
