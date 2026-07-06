package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LectureEnrollmentRepositoryAdapter implements LectureEnrollmentRepository {

    private final LectureEnrollmentJpaRepository jpaRepository;

    @Override
    public LectureEnrollment save(LectureEnrollment e) {
        return jpaRepository.save(e);
    }

    @Override
    public void delete(LectureEnrollment e) {
        jpaRepository.delete(e);
    }

    @Override
    public Optional<List<LectureEnrollment>> findAllByLectureId(Long lectureId) {
        return jpaRepository.findAllByLectureId(lectureId);
    }

    @Override
    public Optional<LectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return jpaRepository.findByLectureIdAndStudentId(lectureId, studentId);
    }
}
