package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.sse.domain.SseEmitterRepository;
import com.example.simplescheduleapp.sse.event.RedisSseMessagePublisher;
import com.example.simplescheduleapp.sse.event.RedisSseMessageSubscriber;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final NotificationService notificationService;
    private final PendingLectureEnrollmentService pendingLectureEnrollmentService;
    private final LectureRepository lectureRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public void requestEnrollment(LectureEnrollmentCreateCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentService.create(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.save(pending);
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        Tutor tutor = lecture.getTutor();
        Long tutorId = tutor.getId();
        String message = "학생이 '" + lecture.getTitle() + "' 강의 수강신청을 요청했습니다.";
        notificationService.sendPushNotification(new NotificationMessage(tutorId, lecture.getTitle(), message));
    }

    public List<LectureEnrollment> getLectureEnrollments(Long lectureId) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentRepository.getAllByLectureId(lectureId);
        if (lectureEnrollments == null || lectureEnrollments.isEmpty()) {
            throw new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
        }
        return lectureEnrollments;
    }

    public void cancelEnrollment(Long studentId, Long lectureId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getByLectureIdAndStudentId(lectureId, studentId);
        LectureEnrollment lectureEnrollment = lectureEnrollmentRepository.getByLectureIdAndStudentId(lectureId, studentId);
        Lecture lecture = lectureEnrollment.getLecture();
        lecture.cancel(lectureEnrollment);
        lectureRepository.save(lecture);
        lectureEnrollmentRepository.delete(lectureEnrollment);
        pendingLectureEnrollmentRepository.delete(pending);
    }
}
