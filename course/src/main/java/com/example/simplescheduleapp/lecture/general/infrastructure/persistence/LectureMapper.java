package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

/**
 * 순수 도메인 {@code Lecture} ↔ JPA {@code LectureEntity} 변환 (ADR-0004).
 *
 * <p><b>version 왕복이 필수다.</b> 낙관적 락(@Version)은 엔티티가 수행하지만, 도메인이 detached
 * 상태로 오가므로 매퍼가 version을 양방향으로 실어 나르지 않으면 merge 시 {@code WHERE version = ?}가
 * 사라져 락이 조용히 무력화된다.
 */
final class LectureMapper {

    private LectureMapper() {
    }

    static Lecture toDomain(LectureEntity entity) {
        return Lecture.reconstitute(
                entity.getId(),
                entity.getVersion(),   // ← 낙관적 락 왕복 (엔티티 → 도메인)
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getMemo(),
                entity.getTutorId(),
                entity.getCapacity(),
                entity.getEnrolledCount());
    }

    static LectureEntity toEntity(Lecture domain) {
        return new LectureEntity(
                domain.getId(),
                domain.getVersion(),   // ← 낙관적 락 왕복 (도메인 → 엔티티)
                domain.getTitle(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getMemo(),
                domain.getTutorId(),
                domain.getCapacity(),
                domain.getEnrolledCount());
    }
}
