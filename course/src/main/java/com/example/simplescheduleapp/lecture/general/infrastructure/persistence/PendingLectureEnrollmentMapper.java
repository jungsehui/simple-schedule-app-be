package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;

/**
 * 순수 도메인 {@code PendingLectureEnrollment} ↔ JPA {@code PendingLectureEnrollmentEntity} 변환 (ADR-0004).
 */
final class PendingLectureEnrollmentMapper {

    private PendingLectureEnrollmentMapper() {
    }

    static PendingLectureEnrollment toDomain(PendingLectureEnrollmentEntity entity) {
        return PendingLectureEnrollment.reconstitute(
                entity.getId(), entity.getLectureId(), entity.getStudentId(), entity.isPermitted());
    }

    static PendingLectureEnrollmentEntity toEntity(PendingLectureEnrollment domain) {
        return new PendingLectureEnrollmentEntity(
                domain.getId(), domain.getLectureId(), domain.getStudentId(), domain.isPermitted());
    }
}
