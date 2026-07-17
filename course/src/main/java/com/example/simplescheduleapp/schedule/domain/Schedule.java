package com.example.simplescheduleapp.schedule.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스케줄 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑(JOINED 상속·discriminator·소프트삭제·감사)은
 * {@code infrastructure/persistence}의 {@code ScheduleEntity}가 담당한다.
 *
 * <p>서브타입(Lecture/SpecialLecture/Consultation)의 공통 상태·불변식을 보유하며,
 * {@code LectureUpdateCommand.toSchedule()}처럼 수정 값 운반체로도 쓰인다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Schedule {

    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String memo;

    /**
     * 3차 방어선: 낙관적 락 버전.
     *
     * <p>순수 도메인이므로 {@code @Version} 애노테이션은 갖지 않는다 — 실제 낙관적 락은
     * {@code ScheduleEntity}의 {@code @Version}이 수행한다. 다만 <b>매퍼가 이 값을 반드시
     * 왕복 복원</b>해야 detached 엔티티 merge 시 {@code WHERE version = ?}가 올바르게 적용된다.
     * 이 왕복이 끊기면 낙관적 락이 조용히 무력화된다(특강 4단계 방어의 3차선).
     */
    private Long version;

    public Schedule(String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        validatePastTime(startTime, endTime);
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
        this.version = 0L;
    }

    /**
     * DB 복원용 — 매퍼/서브타입 전용.
     *
     * <p>이미 저장된 데이터는 재검증하지 않는다(과거 스케줄 로드가 실패하면 안 되므로
     * {@code validatePastTime}을 호출하지 않는다).
     */
    protected Schedule(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }

    protected void updateSchedule(Schedule schedule) {
        this.title = schedule.title;
        this.startTime = schedule.startTime;
        this.endTime = schedule.endTime;
        this.memo = schedule.memo;
    }

    private void validatePastTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ApplicationException(LectureExceptionCode.INVALID_LECTURE_TIME_PAST);
        }
    }
}
