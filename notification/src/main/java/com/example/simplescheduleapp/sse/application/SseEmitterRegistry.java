package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.sse.exception.SseExceptionCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이 인스턴스에 붙어 있는 살아 있는 SSE 커넥션 레지스트리.
 *
 * <p><b>왜 infrastructure가 아닌가.</b> {@link SseEmitter}는 진행 중인 HTTP 응답 그 자체라
 * 프로세스 밖으로 옮길 수 없다 — 외부 시스템을 부르는 어댑터가 아니라 유스케이스가 들고 있는
 * 인메모리 상태다. 여기에 포트 인터페이스를 씌우면 구현이 영원히 하나뿐인 의식만 남는다.
 * 프로세스 경계를 넘는 부분(누가 어디에 붙어 있는가, 알림 브로드캐스트)은 각각
 * {@code SseClientPresence}와 {@code SseMessagePublisher} 포트가 담당한다.
 *
 * <p>이름이 {@code SseEmitterRegistry}였을 때는 애그리게잇을 영속화하는 리포지터리로 읽혔다.
 * 실체는 커넥션 등록부다.
 */
@Component
public class SseEmitterRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(Long memberId, SseEmitter emitter) {
        emitters.put(memberId, emitter);
    }

    public SseEmitter get(Long memberId) {
        return Optional.ofNullable(emitters.get(memberId))
                .orElseThrow(() -> new ApplicationException(SseExceptionCode.SSE_NOT_FOUND));
    }

    public void delete(Long memberId) {
        emitters.remove(memberId);
    }
}
