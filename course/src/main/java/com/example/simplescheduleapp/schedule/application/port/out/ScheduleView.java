package com.example.simplescheduleapp.schedule.application.port.out;

import java.time.LocalDateTime;

/**
 * 캘린더 한 칸 — <b>조회 전용 읽기 모델</b>이다.
 *
 * <p>{@code Schedule} 애그리게잇을 쓰지 않는 이유는 {@link ScheduleType} 때문이다. 종류는
 * 영속 계층의 상속 discriminator에서 나오는 값인데, 그걸 담자고 순수 도메인 모델에 필드를
 * 더하면 애그리게잇이 자기 저장 방식을 알게 된다(ADR-0004가 걷어낸 바로 그것). 조회는 조회
 * 모델로 답하고, 애그리게잇은 쓰기와 불변식에만 쓴다.
 */
public record ScheduleView(
        Long scheduleId,
        ScheduleType type,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo
) {
}
