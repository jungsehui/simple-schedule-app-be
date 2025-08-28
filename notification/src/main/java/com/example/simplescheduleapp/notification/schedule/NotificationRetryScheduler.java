package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Component
public class NotificationRetryScheduler {

    private static final int MAX_RETRY_COUNT = 1;

    private final FailedNotificationRepository failedNotificationRepository;
    private final FcmService fcmService;
    private final TaskExecutor notificationTaskExecutor;

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    public void retryFailedNotifications() {
        List<FailedNotification> targets = failedNotificationRepository.findByRetryCountLessThan(MAX_RETRY_COUNT);

        List<CompletableFuture<Void>> futures = targets.stream()
                .map(failedNotification -> CompletableFuture.runAsync(() -> {
                    try {
                        NotificationMessageEvent event = NotificationMessageEvent.from(failedNotification);
                        fcmService.sendFcmNotification(event);

                        // 성공 시 재시도 목록에서 삭제
                        failedNotificationRepository.delete(failedNotification);
                    } catch (Exception e) {
                        // 재시도 또 실패 시, 카운트 증가
                        failedNotification.incrementRetryCount();
                        failedNotificationRepository.save(failedNotification);
                    }
                }, notificationTaskExecutor))
                .toList();
    }
}
