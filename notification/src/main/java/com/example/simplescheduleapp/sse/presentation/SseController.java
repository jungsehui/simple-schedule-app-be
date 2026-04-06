package com.example.simplescheduleapp.sse.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.sse.application.SseConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SseController {

    private final SseConnectionService sseConnectionService;

    @GetMapping(value = "/sse-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(@Auth Long memberId) {
        log.info("SSE Connection 요청 수신: {}", memberId);
        SseEmitter connectedEmitter = sseConnectionService.connect(memberId);
        log.info("SSE Connection 요청 완료: {}", memberId);
        return connectedEmitter;
    }
}
