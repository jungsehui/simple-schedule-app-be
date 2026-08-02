package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일반 강의 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑은 {@code infrastructure/persistence}의
 * {@code LectureEntity}가 담당한다. 애그리게잇 간 참조는 ID(tutorId)로 한다(Phase A).
 *
 * <p>정원 불변식({@code enrolledCount <= capacity})의 동시 수정 감지는 상위 Schedule의
 * version(낙관적 락)이 담당한다 — 매퍼의 version 왕복이 전제다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Lecture extends Schedule {

    private Long tutorId;
    private int capacity;
    private int enrolledCount;

    public Lecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Long tutorId, int capacity) {
        super(title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
        this.enrolledCount = 0;
    }

    private Lecture(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime, String memo,
                    Long tutorId, int capacity, int enrolledCount) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static Lecture reconstitute(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                                       String memo, Long tutorId, int capacity, int enrolledCount) {
        return new Lecture(id, version, title, startTime, endTime, memo, tutorId, capacity, enrolledCount);
    }

    public LectureEnrollment enroll(Long studentId) {
        increaseEnrolledCount();
        return new LectureEnrollment(getId(), studentId);
    }

    /**
     * 이 강의의 강사인지 확인한다 — 아니면 {@code TUTOR_UNAUTHORIZED}(L2).
     *
     * <p>소유권은 도메인 불변식이므로 애그리거트가 스스로 지킨다. 서비스가 {@code getTutorId()}를
     * 꺼내 비교하면 같은 규칙이 호출처마다 복제된다(ADR-0005).
     */
    public void requireTutor(Long tutorId) {
        validateTutorAuthority(tutorId);
    }

    public void update(Long tutorId, Schedule schedule, int capacity) {
        validateTutorAuthority(tutorId);
        validateCapacityNotBelowEnrolled(capacity);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    private void validateCapacityNotBelowEnrolled(int newCapacity) {
        if (newCapacity < this.enrolledCount) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_BELOW_ENROLLED);
        }
    }

    public void cancel() {
        validateCanDecreaseEnrolledCount();
        this.enrolledCount--;
    }

    private void increaseEnrolledCount() {
        validateCanIncreaseEnrolledCount();
        this.enrolledCount++;
    }

    private void validateTutorAuthority(Long tutorId) {
        if (!this.tutorId.equals(tutorId)) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }

    private void validateCanIncreaseEnrolledCount() {
        if (enrolledCount >= capacity) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    private void validateCanDecreaseEnrolledCount() {
        if (enrolledCount < 1) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_UNDER_ZERO);
        }
    }
}
