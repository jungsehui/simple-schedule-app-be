package com.example.simplescheduleapp.sse.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.sse.application.SseConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SseController {

    private final SseConnectionService sseConnectionService;

    // Phase 3a 듀얼리드: 토큰이 있으면 토큰 식별자 우선. 네이티브 EventSource는 Authorization
    // 헤더를 못 보내므로 경로 변수 폴백은 3b 이후에도 SSE 전용 대안(쿼리 토큰 등) 결정 전까지 유지.
    @GetMapping(value = "/sse-stream/{memberId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(
            @Auth(required = false) Long authMemberId,
            @PathVariable Long memberId
    ) {
        Long resolvedMemberId = AuthIdentities.resolve(authMemberId, memberId);
        log.info("SSE Connection 요청 수신: {}", resolvedMemberId);
        SseEmitter connectedEmitter = sseConnectionService.connect(resolvedMemberId);
        log.info("SSE Connection 요청 완료: {}", resolvedMemberId);
        return connectedEmitter;
    }
}
