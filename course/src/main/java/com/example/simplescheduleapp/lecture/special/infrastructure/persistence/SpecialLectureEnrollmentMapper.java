package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;

/**
 * 순수 도메인 {@code SpecialLectureEnrollment} ↔ JPA {@code SpecialLectureEnrollmentEntity} 변환 (ADR-0004).
 */
final class SpecialLectureEnrollmentMapper {

    private SpecialLectureEnrollmentMapper() {
    }

    static SpecialLectureEnrollment toDomain(SpecialLectureEnrollmentEntity entity) {
        return SpecialLectureEnrollment.reconstitute(
                entity.getId(), entity.getSpecialLectureId(), entity.getStudentId());
    }

    static SpecialLectureEnrollmentEntity toEntity(SpecialLectureEnrollment domain) {
        return new SpecialLectureEnrollmentEntity(
                domain.getId(), domain.getSpecialLectureId(), domain.getStudentId());
    }
}
