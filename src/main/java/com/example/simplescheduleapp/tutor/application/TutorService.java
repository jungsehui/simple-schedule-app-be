package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.sse.domain.SseEmitterRepository;
import com.example.simplescheduleapp.sse.event.RedisSseMessagePublisher;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TutorService {

    private final TutorRegister tutorRegister;
    private final FcmService fcmService;
    private final RedisSseMessagePublisher sseMessagePublisher;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final SseEmitterRepository sseEmitterRepository;

    public Long signUpTutor(TutorSignUpCommand tutorSignUpCommand) {
        Tutor tutor = tutorSignUpCommand.toTutor();
        Tutor registeredTutor = tutorRegister.register(tutor);
        return registeredTutor.getId();
    }

    public void acceptEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        LectureEnrollment lectureEnrollment = lecture.enroll(student);
        lectureEnrollmentRepository.save(lectureEnrollment);
        pending.accept();
        pendingLectureEnrollmentRepository.delete(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 수락되었습니다.";

        // 학생이 현재 접속 중인지 확인
        if (sseEmitterRepository.isConnected(studentId)) {
            // 접속 중이면 SSE 메시지 발행
            sseMessagePublisher.publish(studentId, "enrollment-accepted", message);
        } else {
            // 미접속이면 FCM 전송
            FcmToken studentFcmToken = fcmTokenRepository.getByMemberId(studentId);
            if (studentFcmToken != null) {
                fcmService.sendPushNotification(
                        studentFcmToken.getFcmToken(),
                        "수강신청 수락됨",
                        message
                );
            }
        }
    }

    public void rejectEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        pending.reject();
        pendingLectureEnrollmentRepository.delete(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 거부되었습니다.";

        // 학생이 현재 접속 중인지 확인
        if (sseEmitterRepository.isConnected(studentId)) {
            // 접속 중이면 SSE 메시지 발행
            sseMessagePublisher.publish(studentId, "enrollment-rejected", message);
        } else {
            // 미접속이면 FCM 전송
            FcmToken studentFcmToken = fcmTokenRepository.getByMemberId(studentId);
            if (studentFcmToken != null) {
                fcmService.sendPushNotification(
                        studentFcmToken.getFcmToken(),
                        "수강신청 거부됨",
                        message
                );
            }
        }
    }
}
