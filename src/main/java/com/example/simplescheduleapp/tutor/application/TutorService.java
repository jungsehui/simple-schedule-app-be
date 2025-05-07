package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.lecture.domain.*;
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

    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;
    private final FcmTokenRepository fcmTokenRepository;

    public Long signUpTutor(TutorSignUpCommand tutorSignUpCommand) {
        Tutor tutor = tutorSignUpCommand.toTutor();
        Tutor registeredTutor = tutorRegister.register(tutor);
        return registeredTutor.getId();
    }

    public void acceptEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getById(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        LectureEnrollment lectureEnrollment = lecture.enroll(student);
        lectureEnrollmentRepository.save(lectureEnrollment);
        pending.accept();
        pendingLectureEnrollmentRepository.delete(pending);
        FcmToken studentFcmToken = fcmTokenRepository.getByMemberId(student.getId());
        fcmService.sendPushNotification(
                studentFcmToken.getFcmToken(),
                "수강신청 수락됨",
                "강의 수강신청이 수락되었습니다."
        );
    }

    public void rejectEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Student student = studentRepository.getById(pending.getStudentId());
        pending.reject();
        pendingLectureEnrollmentRepository.delete(pending);
        FcmToken studentFcmToken = fcmTokenRepository.getByMemberId(student.getId());
        fcmService.sendPushNotification(
                studentFcmToken.getFcmToken(),
                "수강신청 거부됨",
                "강의 수강신청이 거부되었습니다."
        );
    }
}
