package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.redis.lock.RedissonDistributedLock;
import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
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

    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;
    private final SpecialLectureEnrollmentRepository enrollmentRepository;

    @RedissonDistributedLock(key = "'specialLecture:' + #command.specialLectureId()")
    public Long enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        SpecialLecture specialLecture = specialLectureRepository.getById(command.specialLectureId());
        Student student = studentRepository.getById(command.studentId());

        SpecialLectureEnrollment enrollment = specialLecture.enroll(student);

        SpecialLectureEnrollment specialLectureEnrollment = enrollmentRepository.save(enrollment);
        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollment.getId();
    }
}
