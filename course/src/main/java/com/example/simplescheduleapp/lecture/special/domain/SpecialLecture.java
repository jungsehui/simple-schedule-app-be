package com.example.simplescheduleapp.lecture.special.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 특강 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑은 {@code infrastructure/persistence}의
 * {@code SpecialLectureEntity}가 담당한다. 애그리게잇 간 참조는 ID(tutorId)로 한다(Phase A).
 *
 * <p>선착순 신청의 3차 방어선(낙관적 락)은 상위 Schedule의 version이 담당한다 —
 * 매퍼의 version 왕복이 전제다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SpecialLecture extends Schedule {

    private Long tutorId;
    private int capacity;

    public SpecialLecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Long tutorId, int capacity) {
        super(title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
    }

    private SpecialLecture(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                           String memo, Long tutorId, int capacity) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static SpecialLecture reconstitute(Long id, Long version, String title, LocalDateTime startTime,
                                              LocalDateTime endTime, String memo, Long tutorId, int capacity) {
        return new SpecialLecture(id, version, title, startTime, endTime, memo, tutorId, capacity);
    }

    public SpecialLectureEnrollment enroll(Long studentId) {
        return new SpecialLectureEnrollment(getId(), studentId);
    }

    public void update(Long tutorId, Schedule schedule, int capacity) {
        validateTutorAuthority(tutorId);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    private void validateTutorAuthority(Long tutorId) {
        if (!this.tutorId.equals(tutorId)) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }
}
