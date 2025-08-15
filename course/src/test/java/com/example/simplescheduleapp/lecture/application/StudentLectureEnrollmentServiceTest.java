package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class StudentLectureEnrollmentServiceTest extends ApplicationTest {

    @Autowired
    private LectureEnrollmentService lectureEnrollmentService;

    @MockitoBean
    private LectureRepository lectureRepository;

    @MockitoBean
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Test
    void 수강신청_조회_시_결과가_없으면_예외를_던진다() {
        // given
        Long lectureIdWithNoEnrollments = 500L;
        given(lectureEnrollmentRepository.getAllByLectureId(lectureIdWithNoEnrollments))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> lectureEnrollmentService.getLectureEnrollments(lectureIdWithNoEnrollments))
                .isInstanceOf(ApplicationException.class)
                .extracting("code")
                .isEqualTo(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
    }

    @Test
    void 수강신청_조회_시_결과가_존재하면_정상_리턴한다() {
        // given
        Long lectureId = 1L;

        // Fixture Monkey를 사용한 테스트 데이터 생성
        Lecture lecture = sut.giveMeBuilder(Lecture.class)
                .set("id", lectureId)
                .sample();

        LectureEnrollment enrollment = sut.giveMeBuilder(LectureEnrollment.class)
                .set("lecture", lecture)
                .sample();

        given(lectureEnrollmentRepository.getAllByLectureId(lectureId))
                .willReturn(List.of(enrollment));

        // when
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentService.getLectureEnrollments(lectureId);

        // then
        assertThat(lectureEnrollments).hasSize(1);
        assertThat(lectureEnrollments.getFirst().getLecture().getId()).isEqualTo(lectureId);
    }
}
