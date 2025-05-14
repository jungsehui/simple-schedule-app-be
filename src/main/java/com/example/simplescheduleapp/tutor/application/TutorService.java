package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
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
    private final LectureEnrollmentService lectureEnrollmentService;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public Long signUpTutor(TutorSignUpCommand tutorSignUpCommand) {
        Tutor tutor = tutorSignUpCommand.toTutor();
        Tutor registeredTutor = tutorRegister.register(tutor);
        return registeredTutor.getId();
    }

    public Long acceptEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        LectureEnrollment lectureEnrollment = lectureEnrollmentService.acceptEnrollment(pending, lecture, student);
        return lectureEnrollment.getId();
    }

    public void rejectEnrollment(Long pendingId) {
        PendingLectureEnrollment pending = pendingLectureEnrollmentRepository.getById(pendingId);
        Lecture lecture = lectureRepository.getByLectureId(pending.getLectureId());
        Student student = studentRepository.getById(pending.getStudentId());
        lectureEnrollmentService.rejectEnrollment(pending, lecture, student);
    }
}
