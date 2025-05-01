package com.example.simplescheduleapp.lecture.application.command;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.lecture.domain.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.domain.repository.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.domain.repository.LectureRepository;
import com.example.simplescheduleapp.student.domain.entity.Student;
import com.example.simplescheduleapp.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LectureEnrollmentService {

    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;

    public void enroll(Long lectureId, Long studentId) {
        Lecture lecture = lectureRepository.getById(lectureId);
        Student student = studentRepository.getById(studentId);

        if (lectureEnrollmentRepository.existsByLectureAndStudent(lecture, student)) {
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        }

        lecture.validateAlreadyEnrolled(student);
        lecture.enrollStudent(student);
        lectureRepository.save(lecture); // cascade 때문에 enrollment 까지 같이 저장됨
    }
}
