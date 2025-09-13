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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.simplescheduleapp.common.config.AsyncConfig.SPECIAL_LECTURE_ENROLLMENT_ASYNC_TASK_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;

    @Async(SPECIAL_LECTURE_ENROLLMENT_ASYNC_TASK_EXECUTOR)
    @Transactional
    public void enrollSpecialLectureEnrollmentAsync(Long specialLectureId, Long studentId) {
        log.info("Writing to DB (Async) -> specialLectureId: {}, studentId: {}", specialLectureId, studentId);
        try {
            SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
            Student student = studentRepository.getById(studentId);
            SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);

            specialLectureEnrollmentRepository.save(specialLectureEnrollment);
            specialLectureRepository.save(specialLecture);
        } catch (DataIntegrityViolationException e) {
            log.warn("Already enrolled student detected in DB writer: {}", studentId);
        } catch (Exception e) {
            log.error("Error while writing enrollment to DB.", e);
        }
    }

    // 퍼사드
    @Transactional
    public SpecialLectureEnrollment enrollSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
        Student student = studentRepository.getById(studentId);
        SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);

        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollmentRepository.save(specialLectureEnrollment);
    }

    // 레디스 호출용
    @Transactional
    public void saveSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        log.info("Writing to DB -> specialLectureId: {}, studentId: {}", specialLectureId, studentId);
        try {
            SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
            Student student = studentRepository.getById(studentId);
            SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);

            specialLectureEnrollmentRepository.save(specialLectureEnrollment);
            specialLectureRepository.save(specialLecture);
        } catch (DataIntegrityViolationException e) {
            log.warn("Already enrolled student detected in DB writer: {}", studentId);
            throw new RuntimeException("이미 신청한 특강입니다.");
        }
    }
}
