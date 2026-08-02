package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;

/**
 * 순수 도메인 {@code SpecialLecture} ↔ JPA {@code SpecialLectureEntity} 변환 (ADR-0004).
 *
 * <p><b>version 왕복이 필수다.</b> 선착순 신청의 3차 방어선(낙관적 락)이 detached 왕복 중
 * 끊기지 않도록 version을 양방향으로 실어 나른다.
 */
final class SpecialLectureMapper {

    private SpecialLectureMapper() {
    }

    static SpecialLecture toDomain(SpecialLectureEntity entity) {
        return SpecialLecture.reconstitute(
                entity.getId(),
                entity.getVersion(),   // ← 낙관적 락 왕복 (엔티티 → 도메인)
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getMemo(),
                entity.getTutorId(),
                entity.getCapacity());
    }

    static SpecialLectureEntity toEntity(SpecialLecture domain) {
        return new SpecialLectureEntity(
                domain.getId(),
                domain.getVersion(),   // ← 낙관적 락 왕복 (도메인 → 엔티티)
                domain.getTitle(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getMemo(),
                domain.getTutorId(),
                domain.getCapacity());
    }
}
