package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRetryScheduler {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int FAILED_NOTIFICATION_PAGE_NUMBER = 0;
    private static final int FAILED_NOTIFICATION_PAGE_SIZE = 50;

    private final FailedNotificationRepository failedNotificationRepository;
    private final NotificationRetryService notificationRetryService;

    // 지금 사용 안 함
    private final Executor failedNotificationExecutor;

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    public void retryFailedNotification() {
        // 페이징 처리
        Pageable limit = PageRequest.of(FAILED_NOTIFICATION_PAGE_NUMBER, FAILED_NOTIFICATION_PAGE_SIZE);
        List<FailedNotification> targets = failedNotificationRepository.findByRetryCountLessThan(MAX_RETRY_COUNT, limit);

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

//    @Scheduled(fixedDelay = 600000) // 10분마다 실행
//    public void retryFailedNotification() {
//        List<FailedNotification> targets = failedNotificationRepository.findByRetryCountLessThan(MAX_RETRY_COUNT);
//
//        if (targets.isEmpty()) {
//            log.info("알림 재처리 대상이 없습니다.");
//            return;
//        }
//
//        log.info("알림 재처리 스케줄을 수행합니다. 대상 총 {}건.", targets.size());
//
//        List<CompletableFuture<Void>> futures = targets.stream()
//                .map(failedNotification -> CompletableFuture.runAsync(() -> {
//                    // ID만 넘기거나, 엔티티를 넘겨서 별도 서비스에서 트랜잭션 처리
//                    notificationRetryService.processSingleRetry(failedNotification.getId());
//                }, failedNotificationExecutor))
//                .toList();
//
//        try {
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//            log.info("알림 재처리 스케줄이 성공적으로 요청되었습니다. 총 {}건 처리.", futures.size());
//        } catch (Exception e) {
//            log.error("알림 재처리 작업 중 일부에서 예외가 발생했습니다.", e);
//        }
//    }
}
