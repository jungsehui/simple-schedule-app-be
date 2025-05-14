package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCancelCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final NotificationService notificationService;
    private final PendingLectureEnrollmentService pendingLectureEnrollmentService;
    private final LectureRepository lectureRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public Long requestEnrollment(LectureEnrollmentCreateCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentService.create(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.save(pending);
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        Tutor tutor = lecture.getTutor();
        String message = "학생이 '" + lecture.getTitle() + "' 강의 수강신청을 요청했습니다.";
        notificationService.sendPushNotification(new NotificationMessage(tutor.getId(), lecture.getTitle(), message));
        return pending.getId();
    }

    public List<LectureEnrollment> getLectureEnrollments(Long lectureId) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentRepository.getAllByLectureId(lectureId);
        if (lectureEnrollments == null || lectureEnrollments.isEmpty()) {
            throw new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
        }
        return lectureEnrollments;
    }

    public void cancelEnrollment(LectureEnrollmentCancelCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        LectureEnrollment lectureEnrollment = lectureEnrollmentRepository.getByLectureIdAndStudentId(command.lectureId(), command.studentId());
        Lecture lecture = lectureEnrollment.getLecture();
        lecture.cancel();
        lectureRepository.save(lecture);
        lectureEnrollmentRepository.delete(lectureEnrollment);
        pendingLectureEnrollmentRepository.delete(pending);
    }

    public LectureEnrollment acceptEnrollment(PendingLectureEnrollment pending, Lecture lecture, Student student) {
        try {
            pending.accept();
            pendingLectureEnrollmentRepository.save(pending);
            LectureEnrollment lectureEnrollment = lecture.enroll(student);
            lectureEnrollmentRepository.save(lectureEnrollment);
            Long studentId = student.getId();
            String message = "'" + lecture.getTitle() + "' 강의 수강신청이 수락되었습니다.";
            notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
            return lectureEnrollment;
        } catch (ApplicationException e) {
            log.error("수강생 등록 에러 메시지: {}", e.getMessage());
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        }
    }

    public void rejectEnrollment(PendingLectureEnrollment pending, Lecture lecture, Student student) {
        pendingLectureEnrollmentRepository.delete(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 거부되었습니다.";
        notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
    }
}
