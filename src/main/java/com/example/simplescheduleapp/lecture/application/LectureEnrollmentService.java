package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
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

    private final FcmService fcmService;
    private final PendingLectureEnrollmentService pendingLectureEnrollmentService;
    private final RedisSseMessagePublisher sseMessagePublisher;
    private final FcmTokenRepository fcmTokenRepository;
    private final LectureRepository lectureRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;
    private final SseEmitterRepository sseEmitterRepository;

    public void requestEnrollment(LectureEnrollmentCreateCommand command) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentService.create(command.lectureId(), command.studentId());
        pendingLectureEnrollmentRepository.save(pending);
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        Tutor tutor = lecture.getTutor();
        Long tutorId = tutor.getId();
        String message = "학생이 '" + lecture.getTitle() + "' 강의 수강신청을 요청했습니다.";

        // 강사가 현재 접속 중인지 확인
        if (sseEmitterRepository.isConnected(tutorId)) {
            // 접속 중이면 SSE 메시지 발행
            sseMessagePublisher.publish(tutorId, "enrollment-request", message);
        } else {
            // 미접속이면 FCM 전송
            FcmToken tutorFcmToken = fcmTokenRepository.getByMemberId(tutorId);
            if (tutorFcmToken != null) {
                fcmService.sendPushNotification(
                        tutorFcmToken.getFcmToken(),
                        "새로운 수강신청 요청",
                        message
                );
            }
        }
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
