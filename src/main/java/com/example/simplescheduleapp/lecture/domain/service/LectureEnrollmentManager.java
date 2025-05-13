package com.example.simplescheduleapp.lecture.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.student.domain.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LectureEnrollmentManager {

    private final NotificationService notificationService;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public LectureEnrollment enrollStudentToLecture(PendingLectureEnrollment pending, Lecture lecture, Student student) {
        try {
            pending.accept();
            pendingLectureEnrollmentRepository.delete(pending);
            lecture.increaseEnrolledCount();
            Long studentId = student.getId();
            LectureEnrollment savedEnrollment = lectureEnrollmentRepository.save(new LectureEnrollment(lecture, student));
            String message = "'" + lecture.getTitle() + "' 강의 수강신청이 수락되었습니다.";
            notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
            return savedEnrollment;
        } catch (DataIntegrityViolationException e) {
            log.error("수강생 등록 에러 메시지: {}", e.getMessage());
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED); // 원본 예외를 포함하여 로깅 및 디버깅에 유용
        }
    }

    public void rejectEnrollment(PendingLectureEnrollment pending, Lecture lecture, Student student) {
        pendingLectureEnrollmentRepository.delete(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 거부되었습니다.";
        notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
    }
}
