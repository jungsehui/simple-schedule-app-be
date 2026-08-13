package com.example.simplescheduleapp.sse.application.port.out;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;

/**
 * SSE 알림 발행 포트 — sse 코어가 소유하고, 구현은 infrastructure가 제공한다.
 *
 * <p>알림 대상이 붙어 있는 인스턴스가 알림을 만든 인스턴스와 다를 수 있어, 발행은 프로세스
 * 경계를 넘는 브로드캐스트여야 한다(현재 Redis pub/sub). 유스케이스는 "이 알림을 구독자에게
 * 흘려보내라"까지만 알고, 그 경로가 무엇인지는 어댑터의 몫이다.
 */
public interface SseMessagePublisher {

    void publish(NotificationRequest event);
}
