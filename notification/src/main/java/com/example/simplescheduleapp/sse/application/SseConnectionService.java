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

    private void clearSseConnectionResource(Long memberId) {
        sseEmitterRepository.delete(memberId);
        redisClientManager.unsubscribeClient(memberId);
        log.debug("SSE 자원 해제 완료 - memberId: {}", memberId);
    }

    private void sendHeartbeat(Long memberId, SseEmitter emitter) {
        taskScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("연결 끊김 방지"));
                redisClientManager.refreshConnection(memberId);
            } catch (IOException e) {
                emitter.complete();
                clearSseConnectionResource(memberId);
            }
            },
                Instant.now().plusSeconds(INITIAL_DELAY),
                Duration.ofSeconds(PERIOD));
    }
}
