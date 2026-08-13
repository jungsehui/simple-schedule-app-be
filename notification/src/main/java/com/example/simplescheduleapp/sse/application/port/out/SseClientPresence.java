package com.example.simplescheduleapp.sse.application.port.out;

/**
 * SSE 클라이언트 접속 현황 포트 — sse 코어가 소유하고, 구현은 infrastructure가 제공한다.
 *
 * <p>이 서비스는 여러 인스턴스로 뜨고 SSE 커넥션은 그중 <b>한 인스턴스에만</b> 붙는다. 그래서
 * "이 회원이 지금 어딘가에 붙어 있는가"는 프로세스 밖(현재 Redis)에서만 답할 수 있고, 그 사실이
 * 이 포트의 존재 이유다 — 구현은 바뀔 수 있지만 유스케이스가 묻는 질문은 바뀌지 않는다.
 *
 * <p>알림을 SSE로 보낼지 FCM 푸시로 보낼지 가르는 분기가 이 답에 걸려 있어
 * ({@code NotificationDispatcher}), notification 슬라이스도 이 포트를 통해 물어본다.
 * 종전에는 양쪽이 Redis 어댑터 구현체를 직접 들고 있었다.
 */
public interface SseClientPresence {

    void subscribeClient(Long memberId);

    void unsubscribeClient(Long memberId);

    boolean isClientConnected(Long memberId);

    void refreshConnection(Long memberId);
}
