package com.example.simplescheduleapp.config;

import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 순수 도메인 서비스의 수동 빈 등록 — 도메인 계층의 Spring 무의존 유지 (ADR-0002)
 */
@Configuration
public class ScheduleConfig {

    @Bean
    public ScheduleConflictValidator scheduleConflictValidator(ScheduleRepository scheduleRepository) {
        return new ScheduleConflictValidator(scheduleRepository);
    }
}
