package com.example.simplescheduleapp.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<FailedNotification> findByRetryCountLessThan(int retryCount);
}
