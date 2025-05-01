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
    void 강사가_학생을_강의에_등록한다() {
    }

    @Test
    void 학생이_강의를_수강취소_한다() {
    }

    @Test
    void 강의_아이디로_해당하는_모든_강의를_찾는다() {
    }

//    @Test
//    void shouldEnrollStudentWhenCapacityAvailable() {
//        // when
//        ArgumentCaptor<LectureEnrollment> captor = ArgumentCaptor.forClass(LectureEnrollment.class);
//        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        LectureEnrollment result = sut.enrollStudentByTutor(1L, 10L, 2L);
//
//        // then
//        verify(enrollmentRepo).save(captor.capture());
//        assertThat(result.getStatus()).isEqualTo(ENROLLED);
//        assertThat(captor.getValue().getLecture().getEnrolledCount()).isEqualTo(1);
//    }
//
//    @Test
//    void shouldAddStudentToWaitingWhenCapacityExceeded() {
//        // given
//        lecture.increaseEnrolledCount(); // 정원 1 → 꽉 참
//
//        // when
//        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        LectureEnrollment result = sut.enrollStudentByTutor(1L, 10L, 2L);
//
//        // then
//        assertThat(result.getStatus()).isEqualTo(WAITING);
//    }
//
//    @Test
//    void shouldThrowWhenAlreadyEnrolled() {
//        // given: 이미 등록된 학생
//        LectureEnrollment existing = new LectureEnrollment(lecture, student, ENROLLED);
//        lecture.getLectureEnrollments().add(existing);
//
//        // when & then
//        assertThatThrownBy(() -> sut.enrollStudentByTutor(1L, 10L, 2L))
//                .isInstanceOf(ApplicationException.class)
//                .hasMessageContaining("ALREADY_ENROLLED");
//    }
//
//    @Test
//    void shouldReturnAllEnrollmentsWithStatus() {
//        // given
//        LectureEnrollment e1 = new LectureEnrollment(lecture, student, ENROLLED);
//        LectureEnrollment e2 = new LectureEnrollment(lecture, new Student(3L, "C"), WAITING);
//        when(enrollmentRepo.findByLectureId(10L)).thenReturn(List.of(e1, e2));
//
//        // when
//        List<LectureEnrollment> result = sut.getEnrollmentsByLectureId(10L);
//
//        // then
//        assertThat(result).hasSize(2);
//        assertThat(result).extracting(LectureEnrollment::getStatus)
//                .containsExactly(ENROLLED, WAITING);
//    }
}