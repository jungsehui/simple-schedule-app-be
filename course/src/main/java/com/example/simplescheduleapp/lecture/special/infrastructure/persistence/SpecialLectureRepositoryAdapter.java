package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code SpecialLectureRepository} 포트의 JPA 어댑터. 도메인은 포트에만 의존하고,
 * Spring Data 세부는 여기에 격리된다. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class SpecialLectureRepositoryAdapter implements SpecialLectureRepository {

    private final SpecialLectureJpaRepository jpaRepository;

    @Override
    public SpecialLecture save(SpecialLecture specialLecture) {
        return jpaRepository.save(specialLecture);
    }

    @Override
    public Optional<SpecialLecture> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
