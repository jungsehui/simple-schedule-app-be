package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code PendingLectureEnrollmentRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>Spring Data/JPA 세부와 도메인↔엔티티 매핑을 여기에 격리한다.
 */
@Repository
@RequiredArgsConstructor
public class PendingLectureEnrollmentRepositoryAdapter implements PendingLectureEnrollmentRepository {

    private final PendingLectureEnrollmentJpaRepository jpaRepository;

    @Override
    public PendingLectureEnrollment save(PendingLectureEnrollment e) {
        return PendingLectureEnrollmentMapper.toDomain(
                jpaRepository.save(PendingLectureEnrollmentMapper.toEntity(e)));
    }

    @Override
    public void delete(PendingLectureEnrollment e) {
        // @SQLDelete(소프트삭제)가 발동하도록 엔티티 삭제 경로를 사용한다.
        jpaRepository.deleteById(e.getId());
    }

    @Override
    public boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return jpaRepository.existsByLectureIdAndStudentId(lectureId, studentId);
    }

    @Override
    public Optional<PendingLectureEnrollment> findById(Long id) {
        return jpaRepository.findById(id).map(PendingLectureEnrollmentMapper::toDomain);
    }

    @Override
    public Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return jpaRepository.findByLectureIdAndStudentId(lectureId, studentId)
                .map(PendingLectureEnrollmentMapper::toDomain);
    }
}
