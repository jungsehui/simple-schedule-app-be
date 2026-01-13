package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;

    public SpecialLectureEnrollment enrollSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
        Student student = studentRepository.getById(studentId);
        SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);
        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollmentRepository.save(specialLectureEnrollment);
    }
}
