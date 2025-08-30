package com.example.simplescheduleapp.notification.application;

import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationFacade {

    private final NotificationService notificationService;
    private final TaskExecutor notificationTaskExecutor;

    /**
     * 여러 학생에게 병렬로 알림을 전송하고 모든 작업이 완료될 때까지 기다립니다.
     * @param senderId 발신자 ID (예: 강의 ID)
     * @param studentIds 수신자 ID 목록
     * @param title 알림 제목
     * @param content 알림 내용
     */
    public void sendNotificationsAsync(Long senderId, List<Long> studentIds, String title, String content) {
        // 3. Executor CompletableFuture 사용
        List<CompletableFuture<Void>> futures = studentIds.stream()
                .map(studentId -> CompletableFuture.runAsync(() -> {
                    try {
                        NotificationMessageEvent event = new NotificationMessageEvent(
                                senderId,
                                studentId,
                                title,
                                content
                        );
                        notificationService.sendPushNotification(event);
                        log.info("Sent notification message. to student ID: {}", studentId);
                    } catch (Exception e) {
                        log.error("Failed to send notification to student ID: {}. Error: {}", studentId, e.getMessage());
                    }
                }, notificationTaskExecutor))
                .toList();

        // 모든 비동기 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
