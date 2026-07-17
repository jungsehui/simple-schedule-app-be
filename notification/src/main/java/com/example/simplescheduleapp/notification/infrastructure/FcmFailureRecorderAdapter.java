package com.example.simplescheduleapp.notification.infrastructure;

import com.example.simplescheduleapp.fcm.application.FcmSendRequest;
import com.example.simplescheduleapp.fcm.application.port.out.FcmFailureRecorder;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * fcm의 {@link FcmFailureRecorder} 포트 구현 — <b>의존성 역전으로 슬라이스 순환을 끊는 지점</b>
 * (ADR-0004, 구조 리뷰).
 *
 * <p>"FCM 전송이 실패했다"는 사실을 재시도 대상({@code FailedNotification})으로 남길지는
 * notification 코어의 정책이므로, 그 결정과 쓰기를 여기서 소유한다. 덕분에 fcm은
 * notification을 전혀 모른 채 실패를 보고만 하면 되고, 의존은 notification → fcm 한 방향만 남는다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FcmFailureRecorderAdapter implements FcmFailureRecorder {

    private final FailedNotificationRepository failedNotificationRepository;

    @Override
    public void recordFailure(FcmSendRequest request, String reason) {
        FailedNotification failedNotification = new FailedNotification(
                request.senderId(),
                request.targetId(),
                request.title(),
                request.body(),
                NotificationType.FCM,
                reason
        );
        failedNotificationRepository.save(failedNotification);
        log.info("실패한 FCM 알림을 재시도 대상으로 저장했습니다. targetId: {}", request.targetId());
    }
}
