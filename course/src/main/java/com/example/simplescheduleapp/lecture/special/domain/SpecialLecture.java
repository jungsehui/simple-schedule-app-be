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

    /**
     * 이 특강이 끝났는가. 신청 마감의 기준이다.
     *
     * <p>기준은 {@code endTime}이다 — {@code startTime}이 아니다. 시작 후 합류를 허용한다
     * (오너 결정 2026-09-04). 클라이언트의 "종료됨" 판정과 같은 기준이라 화면과 서버가 갈리지 않는다.
     *
     * <p><b>{@code now}를 인자로 받는다.</b> 도메인 안에서 {@code LocalDateTime.now()}를 부르면
     * 이 규칙을 시간 이동 없이 검증할 수 없고, 실행 환경의 시계에 조용히 묶인다.
     */
    public boolean hasEnded(LocalDateTime now) {
        return getEndTime().isBefore(now);
    }

    public SpecialLectureEnrollment enroll(Long studentId) {
        return new SpecialLectureEnrollment(getId(), studentId);
    }

    /** 이 특강의 강사인지 확인한다 — 아니면 {@code TUTOR_UNAUTHORIZED}(L2). 소유권은 도메인 불변식이다(ADR-0005). */
    public void requireTutor(Long tutorId) {
        validateTutorAuthority(tutorId);
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
