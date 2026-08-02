package com.example.simplescheduleapp.fcm.application;

/**
 * FCM 전송 요청 — fcm 슬라이스가 <b>스스로 소유하는</b> 입력 계약 (ADR-0004, 구조 리뷰).
 *
 * <p>이전에는 {@code notification.application.event.NotificationRequest}를 그대로 받아
 * fcm → notification 방향 의존을 만들었다(notification → fcm 과 합쳐져 슬라이스 순환).
 * 자기 계약을 소유하면 호출자(notification)가 이 타입으로 변환해 넘기므로 의존은 한 방향만 남는다.
 */
public record FcmSendRequest(
        Long senderId,
        Long targetId,
        String title,
        String body
) {
}
