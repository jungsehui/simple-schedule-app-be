package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import com.example.simplescheduleapp.notification.infrastructure.NotificationRetryScheduler;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.google.api.core.ApiFutures;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// 커넥션 풀 고갈을 Thread.sleep 타이밍으로 유도하는 데드락 재현 테스트 — 환경 의존적이라
// CI 기본 test 태스크에서 제외(@Tag("slow")).
@Tag("slow")
@Slf4j
@DisplayName("Fcm 전송 커넥션 풀 데드락 테스트")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",       // 커넥션 풀 2개
        "spring.datasource.hikari.connection-timeout=2000",   // 대기 시간 2초
        "spring.datasource.hikari.minimum-idle=2"             // 최소 유휴 커넥션 2개
})
class NotificationRetrySchedulerFailureTest extends ApplicationTest {

    @Autowired
    NotificationRetryScheduler notificationRetryScheduler;

    @Autowired
    FailedNotificationRepository failedNotificationRepository;

    // FcmService를 Mock으로 대체하여 지연 시간을 주입
    @MockitoBean
    FcmService fcmService;

    @Test
    void 비동기_처리시_커넥션풀이_작으면_타임아웃_에러가_발생한다() throws InterruptedException {
        // given: 10개의 실패 알림 데이터 생성
        int dataCount = 10;
        for (int i = 0; i < dataCount; i++) {
            failedNotificationRepository.save(new FailedNotification(
                    1L, 2L, "title", "body", NotificationType.FCM, "reason"
            ));
        }

        // given: FCM 전송 요청 시 가정으로 3초 딜레이 발생 (Timeout 2초보다 길게 설정)
        // 이렇게 하면 트랜잭션(@Transactional)을 3초간 유지하게 됨
        given(fcmService.retryFcmNotification(any())).willAnswer(invocation -> {
            log.info("--> FCM 전송 시작 (3초 대기) ...");
            Thread.sleep(3000);
            log.info("<-- FCM 전송 완료");
            return ApiFutures.immediateFuture("test-message-id");
        });

        // when: 스케줄러 실행 (비동기)
        log.info("=== 스케줄러 실행 시작 ===");
        notificationRetryScheduler.retryFailedNotification();

        // 비동기 작업들이 끝날 때까지 충분히 대기
        // 실제로는 CountDownLatch 등을 쓰는 게 좋지만, 테스트 단순화를 위해 sleep 사용
        Thread.sleep(6000);
        log.info("=== 스케줄러 실행 종료 (대기 끝) ===");

        // then: 결과 확인
        // 성공한 개수는 위에서 설정한 커넥션 풀 크기와 같거나 비슷해야 하고 나머지는 타임아웃으로 실패
        // 실패한 경우 카운트가 증가하므로 retry_count > 0 인 데이터를 세어 봄
        long failedCount = failedNotificationRepository.findAll().stream()
                .filter(fn -> fn.getRetryCount() > 0)
                .count();

        // 삭제된 건 성공한 건데 여기선 롤백 등으로 남아있을 수 있음
        long successCount = failedNotificationRepository.count() - failedCount;

        log.info("성공 추정 건수: {}", successCount);
        log.info("실패(타임아웃 등) 건수: {}", failedCount);

        // 검증: 모든 요청이 성공하지 못하고, 일부는 반드시 실패해야 함
        assertThat(failedCount).isGreaterThan(0);
    }
}
