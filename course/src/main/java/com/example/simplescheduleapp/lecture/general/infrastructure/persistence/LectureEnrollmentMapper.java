package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;

/**
 * 순수 도메인 {@code LectureEnrollment} ↔ JPA {@code LectureEnrollmentEntity} 변환 (ADR-0004).
 */
final class LectureEnrollmentMapper {

    private LectureEnrollmentMapper() {
    }

    static LectureEnrollment toDomain(LectureEnrollmentEntity entity) {
        return LectureEnrollment.reconstitute(entity.getId(), entity.getLectureId(), entity.getStudentId());
    }

    static LectureEnrollmentEntity toEntity(LectureEnrollment domain) {
        return new LectureEnrollmentEntity(domain.getId(), domain.getLectureId(), domain.getStudentId());
    }
}
