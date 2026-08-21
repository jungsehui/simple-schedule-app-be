package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface LectureJpaRepository extends JpaRepository<LectureEntity, Long> {

    // JPQL은 엔티티명을 참조하므로 순수화(Lecture → LectureEntity)에 맞춰 갱신 (ADR-0004)
    @Query("SELECT l FROM LectureEntity l WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%")
    List<LectureEntity> findByKeyword(String keyword);

    List<LectureEntity> findAllByTutorId(Long id);

    List<LectureEntity> findAllByIdInOrderByStartTimeAsc(List<Long> ids);
}
