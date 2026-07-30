package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.redis.cache.RedisClientManager;
import com.example.simplescheduleapp.sse.cache.SseEmitterRepository;
import com.example.simplescheduleapp.sse.exception.SseExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static com.example.simplescheduleapp.config.ThreadPoolConfig.SSE_HEARTBEAT_SCHEDULER;

@Slf4j
@RequiredArgsConstructor
@Service
public class SseConnectionService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private static final long INITIAL_DELAY = 10L;
    private static final long PERIOD = 10L;

    @Qualifier(SSE_HEARTBEAT_SCHEDULER)
    private final TaskScheduler taskScheduler;

    private final SseEmitterRepository sseEmitterRepository;
    private final RedisClientManager redisClientManager;

    /**
     * 커넥션별 하트비트 스케줄 핸들. 한 논리적 커넥션의 자원은 emitter·Redis 구독·하트비트 3가지이며,
     * 테어다운 시 <b>셋 다</b> 정리해야 한다. 핸들을 보관하지 않으면 스케줄을 취소할 수 없어
     * 커넥션이 끝나도 하트비트가 영구히 남는다(커넥션당 1개씩 누적).
     */
    private final Map<Long, ScheduledFuture<?>> heartbeats = new ConcurrentHashMap<>();

    public void sendSseNotification(Long targetId, String title, String body) {
        SseEmitter emitter = sseEmitterRepository.get(targetId);

        try {
            emitter.send(SseEmitter.event().name(title).data(body));
            log.info("SSE 이벤트 전송 성공 - targetId: {}, title: {}, body: {}",
                    targetId, title, body);
        } catch (IOException e) {
            log.error("targetId: {} 에게 SSE 이벤트 전송 실패: {}", targetId, body);
            sseEmitterRepository.delete(targetId);
            throw new ApplicationException(SseExceptionCode.SSE_SEND_FAILED);
        }
    }

    public SseEmitter connect(Long memberId) {
        // 재연결 시 이전 커넥션의 하트비트를 먼저 취소한다. 하지 않으면 emitter만 교체되고
        // 이전 하트비트가 살아남아 커넥션마다 스케줄이 누적된다.
        cancelHeartbeat(memberId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        sseEmitterRepository.save(memberId, emitter);
        redisClientManager.subscribeClient(memberId);

        // 콜백 등록 (중복 코드 제거를 위해 clearSseConnectionResource 메서드 활용)
        emitter.onCompletion(() -> clearSseConnectionResource(memberId));

        emitter.onTimeout(() -> {
            emitter.complete();
            clearSseConnectionResource(memberId);
        });

        emitter.onError((ex) -> {
            emitter.completeWithError(ex);
            clearSseConnectionResource(memberId);
        });

        // 초기 연결 메시지 및 하트비트 시작
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connected for memberId: " + memberId));
            sendHeartbeat(memberId, emitter);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /** 한 논리적 커넥션의 자원 3종(하트비트 스케줄·emitter·Redis 구독)을 모두 해제한다. */
    private void clearSseConnectionResource(Long memberId) {
        cancelHeartbeat(memberId);
        sseEmitterRepository.delete(memberId);
        redisClientManager.unsubscribeClient(memberId);
        log.debug("SSE 자원 해제 완료 - memberId: {}", memberId);
    }

    private void cancelHeartbeat(Long memberId) {
        ScheduledFuture<?> heartbeat = heartbeats.remove(memberId);
        if (heartbeat != null) {
            // false: 실행 중인 하트비트를 인터럽트하지 않고 다음 주기부터 멈춘다.
            // (하트비트 자신의 실패 경로에서 호출될 수 있으므로 자기 인터럽트를 피한다)
            heartbeat.cancel(false);
        }
    }

    private void sendHeartbeat(Long memberId, SseEmitter emitter) {
        ScheduledFuture<?> heartbeat = taskScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("연결 끊김 방지"));
                redisClientManager.refreshConnection(memberId);
            } catch (IOException | IllegalStateException e) {
                // IOException: 클라이언트 연결이 끊어져 전송 실패.
                // IllegalStateException: emitter가 이미 완료(완료/타임아웃/에러 콜백 등)되어
                //   "ResponseBodyEmitter has already completed"로 실패 — 재시도해도 영원히 실패하므로
                //   여기서 정리하지 않으면 스케줄이 10초마다 이 예외를 영구히 반복한다.
                emitter.complete();
                clearSseConnectionResource(memberId);
            }
            },
                Instant.now().plusSeconds(INITIAL_DELAY),
                Duration.ofSeconds(PERIOD));

        // 스케줄 핸들을 보관해야 테어다운(완료/타임아웃/에러/재연결)에서 취소할 수 있다.
        heartbeats.put(memberId, heartbeat);
    }
}
