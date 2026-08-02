package com.example.simplescheduleapp.fcm.application.port.out;

import com.example.simplescheduleapp.fcm.application.FcmSendRequest;

/**
 * FCM 전송 실패 보고 포트 — fcm 코어가 소유하고, 구현은 바깥(notification)이 제공한다
 * (ADR-0004, 구조 리뷰: notification↔fcm 슬라이스 순환 제거).
 *
 * <p>이전에는 fcm이 {@code notification.domain}의 {@code FailedNotification} 애그리게잇을 직접
 * 생성·저장했다 — 다른 슬라이스의 도메인에 쓰기를 하며 순환을 만든 지점이다. fcm은 "전송이
 * 실패했다"는 사실만 보고하고, 그것을 어떤 기록으로 남길지는 notification 코어가 정한다.
 */
public interface FcmFailureRecorder {

    void recordFailure(FcmSendRequest request, String reason);
}
