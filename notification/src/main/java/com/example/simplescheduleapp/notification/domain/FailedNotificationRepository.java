package com.example.simplescheduleapp.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {

    List<FailedNotification> findByRetryCountLessThan(int retryCount);
}
