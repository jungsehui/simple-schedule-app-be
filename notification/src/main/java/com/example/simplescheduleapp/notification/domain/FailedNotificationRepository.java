package com.example.simplescheduleapp.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {

    // 페이징 없음
    List<FailedNotification> findByRetryCountLessThan(int retryCount);

    // 페이징 있음
    List<FailedNotification> findByRetryCountLessThan(int retryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fn from FailedNotification fn where fn.id = :id")
    Optional<FailedNotification> findByIdWithLock(@Param("id") Long id);
}
