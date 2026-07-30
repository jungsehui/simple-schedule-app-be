package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.redis.cache.RedisClientManager;
import com.example.simplescheduleapp.sse.cache.SseEmitterRepository;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SseConnectionServiceTest extends UnitTest {

    @InjectMocks
    private SseConnectionService sseConnectionService;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private RedisClientManager redisClientManager;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private ScheduledFuture<?> secondScheduledFuture;

    @DisplayName("emitter가 이미 완료된 상태에서 하트비트가 발사되면 예외를 삼키지 않고 스케줄을 취소 + SSE 자원을 정리한다")
    @Test
    void 완료된_emitter에_하트비트가_발사되면_스케줄을_취소하고_자원을_정리한다() {
        // given: connect()로 SSE 연결을 맺으면 하트비트가 스케줄러(mock)에 등록된다.
        Long memberId = 1L;
        ArgumentCaptor<Runnable> heartbeatTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(heartbeatTaskCaptor.capture(), any(Instant.class), any(Duration.class));

        SseEmitter emitter = sseConnectionService.connect(memberId);

        // 클라이언트 이탈 등으로 emitter가 하트비트보다 먼저 완료된 상황을 재현한다.
        // (실제 서블릿 요청 컨텍스트가 없는 단위 테스트라 emitter.complete()는 onCompletion 콜백을
        //  트리거하지 않고 "완료됨" 상태만 만든다 — 하트비트 실패 경로만 순수하게 검증하기 위함)
        emitter.complete();

        // when: 스케줄러가 다음 주기의 하트비트를 발사했다고 가정하고 캡처한 태스크를 직접 실행한다.
        // 수정 전에는 emitter.send()가 던지는 IllegalStateException("ResponseBodyEmitter has already
        // completed")이 IOException만 잡는 catch 밖으로 새어나가 Spring의 TaskUtils$LoggingErrorHandler가
        // 로그만 남기고 하트비트를 계속 반복시켰다 (실측: 10초마다 반복되어 305회 누적).
        // 수정 후에는 이 예외가 여기서 잡혀 스케줄이 취소되어야 한다.
        assertThatCode(() -> heartbeatTaskCaptor.getValue().run())
                .doesNotThrowAnyException();

        // then: 하트비트 스케줄이 취소되고, SSE 자원(레포지토리 엔트리·Redis 구독)이 해제되어야 한다.
        verify(scheduledFuture, times(1)).cancel(false);
        verify(sseEmitterRepository, times(1)).delete(memberId);
        verify(redisClientManager, times(1)).unsubscribeClient(memberId);
        // send()가 이미 완료 상태에서 실패했으므로 refreshConnection까지는 도달하지 않는다.
        verify(redisClientManager, never()).refreshConnection(memberId);
    }

    @DisplayName("동일 memberId로 재연결하면 이전 커넥션의 하트비트 스케줄을 취소한다")
    @Test
    void 재연결시_이전_커넥션의_하트비트를_취소한다() {
        // given: 같은 memberId로 두 번 연결(재연결)한다고 가정하고, 스케줄러가 매번 다른 핸들을 반환하게 한다.
        Long memberId = 2L;
        doReturn(scheduledFuture, secondScheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));

        // when
        sseConnectionService.connect(memberId);
        sseConnectionService.connect(memberId);

        // then: 두 번째 connect()가 재연결 취소 로직에서 첫 번째 하트비트 핸들을 취소해야 하고,
        // 새로 등록된(두 번째) 핸들은 아직 취소되면 안 된다.
        verify(scheduledFuture, times(1)).cancel(false);
        verify(secondScheduledFuture, never()).cancel(false);
    }
}
