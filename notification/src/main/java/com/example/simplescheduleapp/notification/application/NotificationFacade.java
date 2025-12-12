package com.example.simplescheduleapp.notification.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationFacade {

    private final NotificationDispatcher notificationDispatcher;
    private final Executor notificationExecutor;

    // Target 개수에 따라 단건(Sync/Direct) 처리할지, 다건(Async/Parallel) 처리할지 결정
    public void sendNotification(Long senderId, List<Long> targetIds, String title, String content) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }

        // 1. 대상이 1명이면 -> 단건 전송
        if (targetIds.size() == 1) {
            sendSingle(senderId, targetIds.getFirst(), title, content);
            return;
        }

        // 2. 대상이 2명 이상이면 -> CompletableFuture 이용한 병렬 전송
        sendMultipleAsync(senderId, targetIds, title, content);
    }

    // 편의 메서드
    public void sendNotification(Long senderId, Long targetId, String title, String content) {
        sendSingle(senderId, targetId, title, content);
    }

    private void sendSingle(Long senderId, Long targetId, String title, String content) {
        try {
            NotificationRequest request = new NotificationRequest(senderId, targetId, title, content);
            notificationDispatcher.dispatchPushNotification(request);
            log.info("Sent notification (Single). targetId: {}", targetId);
        } catch (Exception e) {
            log.error("Failed to send notification (Single). targetId: {}", targetId, e);
        }
    }

    private void sendMultipleAsync(Long senderId, List<Long> targetIds, String title, String content) {
        log.info("Starting async multicast notification. Count: {}", targetIds.size());

        List<CompletableFuture<Void>> futures = targetIds.stream()
                .map(targetId -> CompletableFuture.runAsync(() -> {
                    // 내부에서 sendSingle 을 재활용하거나 직접 dispatch 호출
                    sendSingle(senderId, targetId, title, content);
                }, notificationExecutor).exceptionally(e -> {
                    log.error("Failed to send notification to target ID: {}. Error: {}", targetId, e.getMessage());
                    throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
                }))
                .toList();

        // 모든 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Finished async multicast notification.");
    }
}
