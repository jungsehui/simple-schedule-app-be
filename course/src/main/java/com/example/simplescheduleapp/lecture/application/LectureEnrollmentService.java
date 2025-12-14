package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.lecture.application.command.*;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.event.LectureEnrollmentAcceptedEvent;
import com.example.simplescheduleapp.lecture.event.LectureEnrollmentCanceledEvent;
import com.example.simplescheduleapp.lecture.event.LectureEnrollmentRejectedEvent;
import com.example.simplescheduleapp.lecture.event.LectureEnrollmentRequestedEvent;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final PendingLectureEnrollmentService pendingLectureEnrollmentService;

    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long requestEnrollment(LectureEnrollmentCreateCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentService.create(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.save(pending);
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        eventPublisher.publishEvent(new LectureEnrollmentRequestedEvent(pending, lecture));
        return pending.getId();
    }

    public List<LectureEnrollment> getLectureEnrollments(Long lectureId) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentRepository.getAllByLectureId(lectureId);
        if (lectureEnrollments == null || lectureEnrollments.isEmpty()) {
            throw new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
        }
        return lectureEnrollments;
    }

    public void cancelPendingLectureEnrollment(PendingLectureEnrollmentCancelCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.delete(pending);
    }

    @Transactional
    public void cancelLectureEnrollment(LectureEnrollmentCancelCommand command) {
        LectureEnrollment lectureEnrollment = lectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        Lecture lecture = lectureEnrollment.getLecture();
        lecture.cancel();
        lectureRepository.save(lecture);
        lectureEnrollmentRepository.delete(lectureEnrollment);
        eventPublisher.publishEvent(new LectureEnrollmentCanceledEvent(lecture, command.studentId()));
    }

    @Transactional
    public Long acceptEnrollment(PendingAcceptCommand command) {
        try {
            PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(command.pendingId());
            Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
            Student student = studentRepository.getById(pending.getStudentId());
            pending.accept();
            pendingLectureEnrollmentRepository.delete(pending);
            LectureEnrollment lectureEnrollment = lecture.enroll(student);
            lectureEnrollmentRepository.save(lectureEnrollment);
            eventPublisher.publishEvent(new LectureEnrollmentAcceptedEvent(lecture, student));
            return lectureEnrollment.getId();
        } catch (DataIntegrityViolationException e) {
            log.error("이미 등록된 학생입니다. pendingId = {}", command.pendingId());
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        } catch (ApplicationException e) {
            throw e; // 이미 정의된 예외는 그대로 던짐
        } catch (Exception e) {
            log.error("수강생 등록 에러", e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }

    @Transactional
    public void rejectEnrollment(PendingRejectCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(command.pendingId());
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        pending.reject();
        pendingLectureEnrollmentRepository.delete(pending);
        eventPublisher.publishEvent(new LectureEnrollmentRejectedEvent(lecture, student));
    }

    public List<Long> findStudentIdsByLectureId(Long lectureId) {
        return lectureEnrollmentRepository.findAllByLectureId(lectureId)
                .orElse(List.of())
                .stream()
                .map(LectureEnrollment::getStudent) // 먼저 Student 객체를 가져오고
                .filter(Objects::nonNull)           // null이 아닌 Student만 필터링
                .map(Student::getId)                // 안전하게 ID를 가져옴
                .toList();
    }
}
