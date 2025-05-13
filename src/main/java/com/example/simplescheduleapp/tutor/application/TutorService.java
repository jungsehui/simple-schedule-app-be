package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
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
    private final NotificationService notificationService;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

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
        pendingLectureEnrollmentRepository.save(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 수락되었습니다.";
        notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
    }

    public void rejectEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        pending.reject();
        pendingLectureEnrollmentRepository.save(pending);
        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 거부되었습니다.";
        notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));
    }
}
