package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@code LectureEnrollmentRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>Spring Data/JPA 세부와 도메인↔엔티티 매핑을 여기에 격리한다.
 */
@Repository
@RequiredArgsConstructor
public class LectureEnrollmentRepositoryAdapter implements LectureEnrollmentRepository {

    private final LectureEnrollmentJpaRepository jpaRepository;

    @Override
    public LectureEnrollment save(LectureEnrollment e) {
        return LectureEnrollmentMapper.toDomain(jpaRepository.save(LectureEnrollmentMapper.toEntity(e)));
    }

    @Override
    public void delete(LectureEnrollment e) {
        jpaRepository.deleteById(e.getId());
    }

    @Override
    public Optional<List<LectureEnrollment>> findAllByLectureId(Long lectureId) {
        List<LectureEnrollment> enrollments = jpaRepository.findAllByLectureId(lectureId).stream()
                .map(LectureEnrollmentMapper::toDomain)
                .toList();
        // 포트 계약 유지: 결과 없음은 빈 Optional (getAllByLectureId가 도메인 예외로 번역)
        return enrollments.isEmpty() ? Optional.empty() : Optional.of(enrollments);
    }

    @Override
    public Optional<LectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return jpaRepository.findByLectureIdAndStudentId(lectureId, studentId)
                .map(LectureEnrollmentMapper::toDomain);
    }
}
