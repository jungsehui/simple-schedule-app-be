package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class LectureEnrollmentServiceTest {

    @InjectMocks
    private LectureEnrollmentService lectureEnrollmentService;

    @Mock
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private StudentRepository studentRepository;

    Long studentId = 1L;
    Long lectureId = 10L;

    @BeforeEach
    void setUp() {
    }

    @Test
    void 학생이_강의에_등록한다() {
    }

    @Test
    void 강의_아이디로_해당하는_모든_강의를_찾는다() {
    }

    @Test
    void 학생이_강의를_수강취소_한다() {
    }
}