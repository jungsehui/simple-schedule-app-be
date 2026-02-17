package com.example.simplescheduleapp.notification.schedule;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRetryService {

    private final FailedNotificationRepository failedNotificationRepository;
    private final FcmService fcmService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRetry(Long id) {
        failedNotificationRepository.findByIdWithLock(id).ifPresent(failedNotification -> {
            try {
                NotificationRequest request = NotificationRequest.fromFail(failedNotification);
                // 응답 받기
                fcmService.retryFcmNotification(request).get();

                // 성공 시 삭제
                failedNotificationRepository.delete(failedNotification);
            } catch (Exception e) {
                log.warn("알림 재전송 실패. ID: {}, Reason: {}", failedNotification.getId(), e.getMessage());
                // 실패 시 카운트 증가 및 업데이트
                failedNotification.incrementRetryCount();
                failedNotificationRepository.save(failedNotification);
            }
        });
    }
}
