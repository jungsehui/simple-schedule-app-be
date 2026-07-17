package com.example.simplescheduleapp.lecture.general.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강신청 대기 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑(테이블·유니크 제약·소프트삭제)은
 * {@code infrastructure/persistence}의 {@code PendingLectureEnrollmentEntity}가 담당한다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PendingLectureEnrollment {

    private Long id;
    private Long lectureId;
    private Long studentId;
    private boolean permitted;

    public PendingLectureEnrollment(Long lectureId, Long studentId) {
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.permitted = false;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static PendingLectureEnrollment reconstitute(Long id, Long lectureId, Long studentId, boolean permitted) {
        PendingLectureEnrollment pending = new PendingLectureEnrollment(lectureId, studentId);
        pending.id = id;
        pending.permitted = permitted;
        return pending;
    }

    public void accept() {
        this.permitted = true;
    }

    public void reject() {
        this.permitted = false;
    }
}
