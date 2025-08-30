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

    public void sendNotificationsAsync(Long senderMemberId, List<Long> targetMemberIds, String title, String content) {
        // 3. Executor CompletableFuture 사용
        List<CompletableFuture<Void>> futures = targetMemberIds.stream()
                .map(studentId -> CompletableFuture.runAsync(() -> {
                    try {
                        NotificationMessageEvent event = new NotificationMessageEvent(
                                senderMemberId,
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

    public void sendNotification(Long senderId, Long targetId, String title, String content) {
        NotificationMessageEvent event = new NotificationMessageEvent(senderId, targetId, title, content);
        notificationService.sendPushNotification(event);
    }
}
