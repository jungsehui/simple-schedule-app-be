package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRetryScheduler {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int FAILED_NOTIFICATION_BATCH_LIMIT = 50;

    private final FailedNotificationRepository failedNotificationRepository;
    private final NotificationRetryService notificationRetryService;

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    public void retryFailedNotification() {
        // 한 배치당 최대 처리 건수 제한
        List<FailedNotification> targets = failedNotificationRepository.findByRetryCountLessThan(MAX_RETRY_COUNT, FAILED_NOTIFICATION_BATCH_LIMIT);

        if (targets.isEmpty()) {
            log.info("알림 재처리 대상이 없습니다.");
            return;
        }

        log.info("순차 재처리 시작, 대상: {}건", targets.size());

        // DB 커넥션 1개만 사용해서 현재 스레드에서 하나씩 루프
        for (FailedNotification target : targets) {
            try {
                // 영속성 컨텍스트 개별 트랜잭션 처리
                notificationRetryService.processSingleRetry(target.getId());
            } catch (Exception e) {
                log.error("예기치 못한 재처리 실패 ID: {}", target.getId(), e);
            }
        }
    }
}
