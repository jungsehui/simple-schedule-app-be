package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.sse.infrastructure.redis.RedisClientManager;
import com.example.simplescheduleapp.sse.infrastructure.SseEmitterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * SSE 하트비트 스케줄의 라이프사이클 검증 (ADR-0004 / 구조 리뷰 HIGH).
 *
 * <p>한 논리적 커넥션의 자원은 emitter·Redis 구독·하트비트 3가지다. 하트비트 스케줄 핸들
 * ({@code ScheduledFuture})을 보관하지 않으면 커넥션이 끝나도 취소할 수 없어 <b>커넥션마다
 * 하트비트가 영구히 누적</b>되고, 각 태스크가 emitter 참조를 붙들어 메모리도 함께 샌다
 * (CLAUDE.md의 "Emitter 커넥션 고갈 방지" 우려 지점).
 *
 * <p>재연결 경로로 이를 검증한다: 같은 회원이 다시 연결하면 emitter는 교체되지만, 취소가
 * 없으면 이전 하트비트가 살아남는다.
 */
@DisplayName("SSE 하트비트 라이프사이클")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SseHeartbeatLifecycleTest {

    @Mock
    TaskScheduler taskScheduler;

    @Mock
    SseEmitterRepository sseEmitterRepository;

    @Mock
    RedisClientManager redisClientManager;

    @Mock
    ScheduledFuture<Object> firstHeartbeat;

    @Mock
    ScheduledFuture<Object> secondHeartbeat;

    @InjectMocks
    SseConnectionService sseConnectionService;

    @Test
    void 같은_회원이_재연결하면_이전_하트비트_스케줄이_취소된다() {
        // given — 연결마다 서로 다른 스케줄 핸들이 반환된다
        given(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
                .willReturn((ScheduledFuture) firstHeartbeat, (ScheduledFuture) secondHeartbeat);

        // when — 같은 회원이 두 번 연결
        sseConnectionService.connect(1L);
        sseConnectionService.connect(1L);

        // then — 이전 하트비트는 취소되어야 한다(취소가 없으면 커넥션당 스케줄이 영구 누적)
        verify(firstHeartbeat).cancel(false);
    }
}
