package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class TutorServiceTest extends ApplicationTest {

    @Autowired
    private TutorService tutorService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    @MockitoBean
    private LectureRepository lectureRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void 수강등록_수락_처리() {
        // given
        PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
                .set("lectureId", 1L)
                .set("studentId", 2L)
                .sample();

        Lecture lecture = mock(Lecture.class);
        Student student = sut.giveMeOne(Student.class);
        LectureEnrollment enrollment = mock(LectureEnrollment.class);

        given(pendingLectureEnrollmentRepository.getById(anyLong())).willReturn(pending);
        given(lectureRepository.getByLectureId(1L)).willReturn(lecture);
        given(studentRepository.getById(2L)).willReturn(student);
        given(lecture.enroll(student)).willReturn(enrollment);
        given(lecture.getTitle()).willReturn("수학");

        // when
        tutorService.acceptEnrollment(99L);

        // then
        then(lectureEnrollmentRepository).should().save(enrollment);
        then(pendingLectureEnrollmentRepository).should().delete(pending);
        then(notificationService).should().sendPushNotification(
                new NotificationMessage(student.getId(), "수학", "'수학' 강의 수강신청이 수락되었습니다."));
    }

    @Test
    void 수강등록_거부_처리() {
        // given
        PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
                .set("lectureId", 1L)
                .set("studentId", 2L)
                .sample();

        Lecture lecture = mock(Lecture.class);
        Student student = sut.giveMeOne(Student.class);

        given(pendingLectureEnrollmentRepository.getById(anyLong())).willReturn(pending);
        given(lectureRepository.getByLectureId(1L)).willReturn(lecture);
        given(studentRepository.getById(2L)).willReturn(student);
        given(lecture.getTitle()).willReturn("영어");

        // when
        tutorService.rejectEnrollment(88L);

        // then
        then(pendingLectureEnrollmentRepository).should().delete(pending);
        then(notificationService).should().sendPushNotification(
                new NotificationMessage(student.getId(), "영어", "'영어' 강의 수강신청이 거부되었습니다."));
    }
}
