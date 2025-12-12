package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRetryScheduler {

    private static final int MAX_RETRY_COUNT = 4; // MAX_RETRY_COUNT 미만만큼 수행하므로 지금은 3번만 수행

    private final FailedNotificationRepository failedNotificationRepository;
    private final NotificationRetryService notificationRetryService;
    private final Executor notificationExecutor;

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    public void retryFailedNotifications() {
        List<FailedNotification> targets = failedNotificationRepository.findByRetryCountLessThan(MAX_RETRY_COUNT);

        if (targets.isEmpty()) {
            log.info("알림 재처리 대상이 없습니다.");
            return;
        }

        log.info("알림 재처리 스케줄을 수행합니다. 대상 총 {}건.", targets.size());

        List<CompletableFuture<Void>> futures = targets.stream()
                .map(failedNotification -> CompletableFuture.runAsync(() -> {
                    // ID만 넘기거나, 엔티티를 넘겨서 별도 서비스에서 트랜잭션 처리
                    notificationRetryService.processSingleRetry(failedNotification.getId());
                }, notificationExecutor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("알림 재처리 스케줄이 성공적으로 요청되었습니다. 총 {}건 처리.", futures.size());
        } catch (Exception e) {
            log.error("알림 재처리 작업 중 일부에서 예외가 발생했습니다.", e);
        }
    }
}
