package com.example.simplescheduleapp.sse.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.sse.exception.SseExceptionCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRepository {

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

    public boolean isConnected(Long memberId) {
        return emitters.containsKey(memberId);
    }
}
