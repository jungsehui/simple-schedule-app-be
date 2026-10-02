package com.example.simplescheduleapp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링·재시도 전역 활성화 (ADR-0003 Stage 2).
 * <p>
 * course(OutboxRelayScheduler, @Retryable)와 notification(NotificationRetryScheduler)이 각각
 * 필요로 하던 {@code @EnableScheduling}과 재시도 활성화를 공유 모듈 common에 단일 정의한다.
 * 재시도는 Boot 4에서 spring-retry({@code @EnableRetry})가 빠져 Framework 7 네이티브
 * {@code @EnableResilientMethods}로 바뀌었다(ADR-0003 Stage 3).
 * 두 모듈이 독립 Gradle 모듈이라 각자 테스트 컨텍스트에도 이 설정이 필요하며, common에 두면
 * course·notification·:app 어디서나 정확히 1개의 빈으로 존재해 통합 시 빈 이름 충돌이 없다.
 */
@EnableScheduling
@EnableResilientMethods
@Configuration
public class SchedulingRetryConfig {
}
