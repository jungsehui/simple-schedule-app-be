package com.example.simplescheduleapp.lecture.general.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.event.LectureEnrollmentAcceptedEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentCanceledEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentRejectedEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentRequestedEvent;
import com.example.simplescheduleapp.lecture.general.application.command.*;
import com.example.simplescheduleapp.lecture.general.application.result.LectureEnrollmentDetail;
import com.example.simplescheduleapp.lecture.general.domain.*;
import com.example.simplescheduleapp.lecture.general.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final PendingLectureEnrollmentService pendingLectureEnrollmentService;
    private final ScheduleConflictValidator scheduleConflictValidator;

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

    @Transactional
    public void cancelPendingLectureEnrollment(PendingLectureEnrollmentCancelCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.delete(pending);
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 2, delay = 100)
    @Transactional
    public void cancelLectureEnrollment(LectureEnrollmentCancelCommand command) {
        LectureEnrollment lectureEnrollment = lectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        // 애그리게잇 관통(enrollment→lecture) 대신 Lecture 애그리게잇을 리포지토리로 직접 로드 (ADR-0004 Phase A)
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        lecture.cancel();
        lectureRepository.save(lecture);
        lectureEnrollmentRepository.delete(lectureEnrollment);
        eventPublisher.publishEvent(new LectureEnrollmentCanceledEvent(lecture, command.studentId()));
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 2, delay = 100)
    @Transactional
    public Long acceptEnrollment(PendingAcceptCommand command) {
        try {
            PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(command.pendingId());
            Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
            lecture.requireTutor(command.memberId());
            Student student = studentRepository.getById(pending.getStudentId());
            scheduleConflictValidator.validateNoStudentConflict(student.getId(), lecture.getStartTime(), lecture.getEndTime(), null);
            pending.accept();
            pendingLectureEnrollmentRepository.delete(pending);
            LectureEnrollment lectureEnrollment = lecture.enroll(student.getId());
            lectureEnrollmentRepository.save(lectureEnrollment);
            eventPublisher.publishEvent(new LectureEnrollmentAcceptedEvent(lecture, student));
            return lectureEnrollment.getId();
        } catch (DataIntegrityViolationException e) {
            log.error("이미 등록된 학생입니다. pendingId = {}", command.pendingId());
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        }
    }

    @Transactional
    public void rejectEnrollment(PendingRejectCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(command.pendingId());
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        lecture.requireTutor(command.memberId());
        Student student = studentRepository.getById(pending.getStudentId());
        pending.reject();
        pendingLectureEnrollmentRepository.delete(pending);
        eventPublisher.publishEvent(new LectureEnrollmentRejectedEvent(lecture, student));
    }


    public List<Long> findStudentIdsByLectureId(Long lectureId) {
        return lectureEnrollmentRepository.findAllByLectureId(lectureId)
                .orElse(List.of())
                .stream()
                .map(LectureEnrollment::getStudentId) // ID 참조라 객체 순회 불필요 (ADR-0004 Phase A)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 수강생 조회 화면용 상세 — 애그리게잇 간 참조가 ID이므로 관련 애그리게잇(Lecture·Student)을
     * 애플리케이션 계층이 로드해 조립한다. 프레젠테이션은 도메인 그래프를 순회하지 않는다.
     */
    /**
     * 강의의 수강생 명단. <b>해당 강의의 강사만</b> 볼 수 있다 (ADR-0005).
     *
     * <p>이전에는 인증조차 없어 강의 ID만 알면 누구나 수강생 명단을 조회할 수 있었다.
     */
    public LectureEnrollmentDetail getLectureEnrollmentDetail(Long memberId, Long lectureId) {
        Lecture lecture = lectureRepository.getByLectureId(lectureId);
        lecture.requireTutor(memberId);
        List<LectureEnrollment> lectureEnrollments = getLectureEnrollments(lectureId);
        List<Student> students = lectureEnrollments.stream()
                .map(LectureEnrollment::getStudentId)
                .filter(Objects::nonNull)
                .map(studentRepository::getById)
                .toList();
        return new LectureEnrollmentDetail(lecture, students);
    }
}
