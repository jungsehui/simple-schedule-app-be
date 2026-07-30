package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code ScheduleRepository} 포트의 JPA 어댑터. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryAdapter implements ScheduleRepository {

    /**
     * schedule_id는 IDENTITY 생성 전략(1부터 증가하는 양수)이므로, "제외할 스케줄 없음"을 뜻하는
     * sentinel로 안전하게 쓸 수 있다. 네이티브 쿼리에 NULL 바인드 파라미터를 절대 넘기지 않기 위한 장치로,
     * PostgreSQL의 "could not determine data type of parameter" 오류와 MySQL의
     * "CAST(... AS BIGINT)" 문법 오류를 동시에 방지한다.
     */
    private static final long NO_EXCLUDE_SCHEDULE_ID = -1L;

    private final ScheduleJpaRepository jpaRepository;

    @Override
    public List<Long> findOverlappingScheduleIdsByTutorId(
            Long tutorId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        return jpaRepository.findOverlappingScheduleIdsByTutorId(
                tutorId, startTime, endTime, toExcludeId(excludeScheduleId));
    }

    @Override
    public List<Long> findOverlappingScheduleIdsByStudentId(
            Long studentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        return jpaRepository.findOverlappingScheduleIdsByStudentId(
                studentId, startTime, endTime, toExcludeId(excludeScheduleId));
    }

    private long toExcludeId(Long excludeScheduleId) {
        return excludeScheduleId != null ? excludeScheduleId : NO_EXCLUDE_SCHEDULE_ID;
    }
}
