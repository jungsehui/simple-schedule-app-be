package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollmentRepository;
import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TutorService {

    private final TutorRegister tutorRegister;
    private final LectureEnrollmentService lectureEnrollmentService;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public Long signUpTutor(TutorSignUpCommand tutorSignUpCommand) {
        Tutor tutor = tutorSignUpCommand.toTutor();
        Tutor registeredTutor = tutorRegister.register(tutor);
        return registeredTutor.getId();
    }

    public Long acceptEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        LectureEnrollment lectureEnrollment = lectureEnrollmentService.acceptEnrollment(pending);
        return lectureEnrollment.getId();
    }

    public void rejectEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        lectureEnrollmentService.rejectEnrollment(pending);
    }
}
