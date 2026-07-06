package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code SpecialLectureEnrollmentRepository} 포트의 JPA 어댑터. 도메인은 포트에만 의존하고,
 * Spring Data 세부는 여기에 격리된다. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class SpecialLectureEnrollmentRepositoryAdapter implements SpecialLectureEnrollmentRepository {

    private final SpecialLectureEnrollmentJpaRepository jpaRepository;

    @Override
    public SpecialLectureEnrollment save(SpecialLectureEnrollment specialLectureEnrollment) {
        return jpaRepository.save(specialLectureEnrollment);
    }

    @Override
    public Optional<SpecialLectureEnrollment> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
