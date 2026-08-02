package com.example.simplescheduleapp.lecture.special.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특강 수강등록 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑(테이블·유니크 제약·NOT NULL)은
 * {@code infrastructure/persistence}의 {@code SpecialLectureEnrollmentEntity}가 담당한다.
 * 애그리게잇 간 참조는 ID로 한다(ADR-0004 Phase A).
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SpecialLectureEnrollment {

    private Long id;
    private Long specialLectureId;
    private Long studentId;

    public SpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        this.specialLectureId = specialLectureId;
        this.studentId = studentId;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static SpecialLectureEnrollment reconstitute(Long id, Long specialLectureId, Long studentId) {
        SpecialLectureEnrollment enrollment = new SpecialLectureEnrollment(specialLectureId, studentId);
        enrollment.id = id;
        return enrollment;
    }
}
