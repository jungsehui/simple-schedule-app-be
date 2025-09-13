package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void enrollSpecialLectureEnrollmentAsync(Long specialLectureId, Long studentId) {
        log.info("Writing to DB (Async) -> specialLectureId: {}, studentId: {}", specialLectureId, studentId);
        try {
            int updatedRows = specialLectureRepository.increaseSpecialLectureEnrollmentCount(specialLectureId);
            if (updatedRows == 0) {
                log.error("DB capacity check failed for specialLectureId: {}", specialLectureId);
                return;
            }

            SpecialLecture specialLectureRef = specialLectureRepository.getReferenceById(specialLectureId);
            Student studentRef = studentRepository.getReferenceById(studentId);
            SpecialLectureEnrollment enrollment = new SpecialLectureEnrollment(specialLectureRef, studentRef);
            specialLectureEnrollmentRepository.save(enrollment);

        } catch (DataIntegrityViolationException e) {
            log.warn("Already enrolled student detected in DB writer: {}", studentId);
        } catch (Exception e) {
            log.error("Error while writing enrollment to DB.", e);
        }
    }
}
