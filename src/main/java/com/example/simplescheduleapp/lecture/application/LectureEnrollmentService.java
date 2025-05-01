package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCommand;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;

    public LectureEnrollment enroll(LectureEnrollmentCommand command) {
        Student student = studentRepository.getById(command.studentId());
        Lecture lecture = lectureRepository.getById(command.lectureId());
        LectureEnrollment lectureEnrollment = lecture.enroll(student);
        return lectureEnrollmentRepository.save(lectureEnrollment);
    }

    public List<LectureEnrollment> getLectureEnrollments(Long lectureId) {
        return lectureEnrollmentRepository.getAllByLectureId(lectureId);
    }

    public void cancelEnrollment(Long lectureId, Long studentId) {
        LectureEnrollment lectureEnrollment = lectureEnrollmentRepository.getByLectureIdAndStudentId(lectureId, studentId);
        Lecture lecture = lectureEnrollment.getLecture();
        lecture.decreaseEnrolledCount();
        lectureRepository.save(lecture);
        lectureEnrollmentRepository.delete(lectureEnrollment);
    }
}
