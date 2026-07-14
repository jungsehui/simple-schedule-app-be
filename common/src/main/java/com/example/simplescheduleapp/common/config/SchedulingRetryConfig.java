package com.example.simplescheduleapp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링·재시도 전역 활성화 (ADR-0003 Stage 2).
 * <p>
 * course(OutboxRelayScheduler, @Retryable)와 notification(NotificationRetryScheduler)이 각각
 * 필요로 하던 {@code @EnableScheduling}/{@code @EnableRetry}를 공유 모듈 common에 단일 정의한다.
 * 두 모듈이 독립 Gradle 모듈이라 각자 테스트 컨텍스트에도 이 설정이 필요하며, common에 두면
 * course·notification·:app 어디서나 정확히 1개의 빈으로 존재해 통합 시 빈 이름 충돌이 없다.
 */
@EnableScheduling
@EnableRetry
@Configuration
public class SchedulingRetryConfig {
}
