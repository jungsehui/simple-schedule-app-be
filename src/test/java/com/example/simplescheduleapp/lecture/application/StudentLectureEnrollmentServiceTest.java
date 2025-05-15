package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class StudentLectureEnrollmentServiceTest extends ApplicationTest {

    @Autowired
    private LectureEnrollmentService lectureEnrollmentService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private FcmTokenRepository fcmTokenRepository;

    @MockitoBean
    private LectureRepository lectureRepository;

    @MockitoBean
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @MockitoBean
    private TutorRepository tutorRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    Long tutorId = 1L;
    Long studentId = 1L;
    Long lectureId = 1L;
    Long fcmTokenId = 1L;

    Tutor tutorSut = sut.giveMeBuilder(Tutor.class)
            .set("id", tutorId)
            .sample();

    Student studentSut = sut.giveMeBuilder(Student.class)
            .set("id", studentId)
            .sample();

    Lecture lectureSut = sut.giveMeBuilder(Lecture.class)
            .set("id", lectureId)
            .set("tutor", tutorSut)
            .set("title", "수학의 정석") // 정상 문자열 지정
            .sample();

    FcmToken fcmTokenSut = sut.giveMeBuilder(FcmToken.class)
            .set("id", fcmTokenId)
            .set("member", tutorSut)
            .set("fcmToken", "dummy-token")
            .sample();

    @BeforeEach
    void setUp() {
        given(tutorRepository.getById(tutorId)).willReturn(tutorSut);
        given(studentRepository.getById(studentId)).willReturn(studentSut);
        given(lectureRepository.getByLectureId(lectureId)).willReturn(lectureSut);
        given(fcmTokenRepository.getByMemberId(fcmTokenId)).willReturn(fcmTokenSut);
    }

    @Test
    void 수강신청_요청_시_알림이_전송된다() {
        // given
        LectureEnrollmentCreateCommand command = new LectureEnrollmentCreateCommand(studentId, lectureId);

        // when
        lectureEnrollmentService.requestEnrollment(command);

        // then
        verify(notificationService).sendPushNotification(
                eq(new NotificationMessage(
                        1L,
                        lectureSut.getTitle(),
                        "학생이 '" + lectureSut.getTitle() + "' 강의 수강신청을 요청했습니다."
                ))
        );
    }

    @Test
    void 수강신청_조회_시_결과가_없으면_예외를_던진다() {
        // given
        Long lectureId = 500L;
        when(lectureEnrollmentRepository.getAllByLectureId(lectureId)).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> lectureEnrollmentService.getLectureEnrollments(lectureId))
                .isInstanceOf(ApplicationException.class)
                .extracting("code")
                .isEqualTo(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
    }

    @Test
    void 수강신청_조회_시_결과가_존재하면_정상_리턴한다() {
        // given
        LectureEnrollment enrollment1 = sut.giveMeBuilder(LectureEnrollment.class)
                .set("lecture", lectureSut)
                .sample();

        when(lectureEnrollmentRepository.getAllByLectureId(lectureId))
                .thenReturn(List.of(enrollment1));

        // when
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentService.getLectureEnrollments(lectureId);

        // then
        assertThat(lectureEnrollments).hasSize(1);
        assertThat(lectureEnrollments.getFirst().getLecture().getId()).isEqualTo(lectureId);
    }
}
