package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;
    private final ScheduleConflictValidator scheduleConflictValidator;

    @Transactional
    public SpecialLectureEnrollment enrollSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
        Student student = studentRepository.getById(studentId);
        scheduleConflictValidator.validateNoStudentConflict(student.getId(), specialLecture.getStartTime(), specialLecture.getEndTime(), null);
        SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);
        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollmentRepository.save(specialLectureEnrollment);
    }
}
