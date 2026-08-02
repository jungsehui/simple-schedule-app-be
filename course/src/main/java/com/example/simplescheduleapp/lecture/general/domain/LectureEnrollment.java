package com.example.simplescheduleapp.lecture.general.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강등록 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑(테이블·유니크 제약·NOT NULL)은
 * {@code infrastructure/persistence}의 {@code LectureEnrollmentEntity}가 담당한다.
 * 애그리게잇 간 참조는 ID로 한다(ADR-0004 Phase A).
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LectureEnrollment {

    private Long id;
    private Long lectureId;
    private Long studentId;

    public LectureEnrollment(Long lectureId, Long studentId) {
        this.lectureId = lectureId;
        this.studentId = studentId;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static LectureEnrollment reconstitute(Long id, Long lectureId, Long studentId) {
        LectureEnrollment enrollment = new LectureEnrollment(lectureId, studentId);
        enrollment.id = id;
        return enrollment;
    }
}
