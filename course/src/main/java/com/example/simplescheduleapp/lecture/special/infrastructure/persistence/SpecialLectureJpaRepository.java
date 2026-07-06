package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 리포지토리 — {@link SpecialLectureRepositoryAdapter}가 이 인터페이스로
 * {@code SpecialLectureRepository} 포트를 구현한다. Spring Data는 이 infrastructure 계층에만 존재한다.
 */
public interface SpecialLectureJpaRepository extends JpaRepository<SpecialLecture, Long> {
}
