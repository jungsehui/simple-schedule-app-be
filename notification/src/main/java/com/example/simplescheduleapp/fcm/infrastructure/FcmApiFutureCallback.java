package com.example.simplescheduleapp.fcm.infrastructure;

import com.example.simplescheduleapp.fcm.application.FcmSendRequest;
import com.example.simplescheduleapp.fcm.application.port.out.FcmFailureRecorder;
import com.google.api.core.ApiFutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase 비동기 전송 콜백 어댑터.
 *
 * <p>실패 시 {@link FcmFailureRecorder} 포트로 보고만 한다 — 이전에는 여기서
 * {@code notification.domain}의 {@code FailedNotification}을 직접 만들어 저장해
 * fcm → notification 역방향 의존(슬라이스 순환)을 만들었다. (ADR-0004)
 */
@Slf4j
@RequiredArgsConstructor
public class FcmApiFutureCallback implements ApiFutureCallback<String> {

    private final FcmSendRequest event;
    private final String fcmToken;
    private final FcmFailureRecorder fcmFailureRecorder;

    @Override
    public void onFailure(Throwable t) {
        log.error("FCM 비동기 전송 실패 - target member Id: {}, token: {}, exception: {}",
                event.targetId(), fcmToken, t.getMessage());

        fcmFailureRecorder.recordFailure(event, t.getMessage());
        log.info("FCM 전송 실패를 보고했습니다. targetId: {}", event.targetId());
    }

    @Override
    public void onSuccess(String result) {
        log.info("FCM 비동기 전송 성공 - result: {}, target member ID: {}",
                result, event.targetId());
    }
}
