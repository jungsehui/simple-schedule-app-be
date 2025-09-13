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

    // 퍼사드
    @Transactional
    public SpecialLectureEnrollment enrollSpecialLectureEnrollment(Long lectureId, Long studentId) {
        SpecialLecture specialLecture = specialLectureRepository.getById(lectureId);
        Student student = studentRepository.getById(studentId);

        SpecialLectureEnrollment enrollment = specialLecture.enroll(student);

        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollmentRepository.save(enrollment);
    }

    // 레디스 호출용
    @Transactional
    public void saveSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        log.info("Writing to DB -> specialLectureId: {}, studentId: {}", specialLectureId, studentId);
        try {
            int updatedRows = specialLectureRepository.increaseSpecialLectureEnrollmentCount(specialLectureId);
            if (updatedRows == 0) {
                log.error("DB capacity check failed for specialLectureId: {}", specialLectureId);
                // Redis에서 이미 성공했으므로, 이 경우는 거의 발생하지 않지만 안전장치로 남겨둠.
                // 실제로는 보상 트랜잭션을 통해 Redis 카운트를 원복해야 함.
                throw new RuntimeException("DB 저장 단계에서 정원 초과가 확인되었습니다.");
            }

            SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
            Student student = studentRepository.getById(studentId);
            SpecialLectureEnrollment specialLectureEnrollment = new SpecialLectureEnrollment(specialLecture, student);

            specialLectureEnrollmentRepository.save(specialLectureEnrollment);
            specialLectureRepository.save(specialLecture);
        } catch (DataIntegrityViolationException e) {
            log.warn("Already enrolled student detected in DB writer: {}", studentId);
            throw new RuntimeException("이미 신청한 특강입니다.");
        }
    }
}
