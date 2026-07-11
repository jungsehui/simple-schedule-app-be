package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PendingLectureEnrollmentRepositoryAdapter implements PendingLectureEnrollmentRepository {

    private final PendingLectureEnrollmentJpaRepository jpaRepository;

    @Override
    public PendingLectureEnrollment save(PendingLectureEnrollment e) {
        return jpaRepository.save(e);
    }

    @Override
    public void delete(PendingLectureEnrollment e) {
        jpaRepository.delete(e);
    }

    @Override
    public boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return jpaRepository.existsByLectureIdAndStudentId(lectureId, studentId);
    }

    @Override
    public Optional<PendingLectureEnrollment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long studentId, Long lectureId) {
        return jpaRepository.findByLectureIdAndStudentId(studentId, lectureId);
    }
}
