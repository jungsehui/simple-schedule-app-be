package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.command.PendingAcceptCommand;
import com.example.simplescheduleapp.lecture.general.application.command.PendingRejectCommand;
import com.example.simplescheduleapp.lecture.general.domain.*;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationWithKafkaTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class TutorLectureEnrollmentServiceTest extends ApplicationWithKafkaTest {

    @Autowired
    private LectureEnrollmentService lectureEnrollmentService;

    @Autowired
    private ObjectMapper objectMapper;

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
        long tutorId = 100L;
        long studentId = 2L;
        String lectureTitle = "수학";

        // Phase 3a: 토큰 식별자(소유 튜터)와 함께 수락 — 소유권 검증 통과 경로
        PendingAcceptCommand command = new PendingAcceptCommand(tutorId, 99L);
        PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
                .set("lectureId", 1L)
                .set("studentId", studentId)
                .sample();

        Lecture lecture = mock(Lecture.class);
        Student student = mock(Student.class);
        LectureEnrollment enrollment = mock(LectureEnrollment.class);
        Tutor tutor = mock(Tutor.class);

        given(pendingLectureEnrollmentRepository.getById(anyLong())).willReturn(pending);
        given(lectureRepository.getByLectureId(1L)).willReturn(lecture);
        given(studentRepository.getById(studentId)).willReturn(student);
        given(lecture.enroll(student)).willReturn(enrollment);
        given(lecture.getTitle()).willReturn(lectureTitle);
        given(lecture.getTutor()).willReturn(tutor);
        given(tutor.getId()).willReturn(tutorId);
        given(student.getId()).willReturn(studentId);

        // when
        lectureEnrollmentService.acceptEnrollment(command);

        // then
        // Kafka 메시지 소비 및 검증
        ConsumerRecords<String, String> records = waitingConsumeTopicSync(KafkaTopics.COURSE_EVENT_TOPIC);
        assertThat(records.count()).isEqualTo(1); // 1개의 메시지만 소비되었는지 확인

        ConsumerRecord<String, String> record = records.iterator().next();
        KafkaLectureEventMessage message = objectMapper.readValue(record.value(), KafkaLectureEventMessage.class);

        assertThat(message.type()).isEqualTo(LectureEventType.ENROLLMENT_ACCEPTED);
        assertThat(message.tutorId()).isEqualTo(tutorId);
        assertThat(message.studentId()).isEqualTo(studentId);
        assertThat(message.lectureTitle()).isEqualTo(lectureTitle);

        // 기존 DB 검증 유지
        then(lectureEnrollmentRepository).should().save(enrollment);
        then(pendingLectureEnrollmentRepository).should().delete(pending);
    }

    @Test
    void 수강등록_거부_처리() {
        // given
        long tutorId = 100L;
        long studentId = 2L;
        String lectureTitle = "영어";

        // Phase 3a: 토큰 식별자(소유 튜터)와 함께 거절 — 소유권 검증 통과 경로
        PendingRejectCommand command = new PendingRejectCommand(tutorId, 88L);
        PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
                .set("lectureId", 1L)
                .set("studentId", studentId)
                .sample();

        Lecture lecture = mock(Lecture.class);
        Student student = mock(Student.class);
        Tutor tutor = mock(Tutor.class);

        given(pendingLectureEnrollmentRepository.getById(anyLong())).willReturn(pending);
        given(lectureRepository.getByLectureId(1L)).willReturn(lecture);
        given(studentRepository.getById(studentId)).willReturn(student);
        given(lecture.getTitle()).willReturn(lectureTitle);
        given(lecture.getTutor()).willReturn(tutor);
        given(tutor.getId()).willReturn(tutorId);
        given(student.getId()).willReturn(studentId); // 거부 메시지에도 학생 ID가 필요할 수 있으므로 추가

        // when
        lectureEnrollmentService.rejectEnrollment(command);

        // then
        ConsumerRecords<String, String> records = waitingConsumeTopicSync(KafkaTopics.COURSE_EVENT_TOPIC);
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        KafkaLectureEventMessage message = objectMapper.readValue(record.value(), KafkaLectureEventMessage.class);

        assertThat(message.type()).isEqualTo(LectureEventType.ENROLLMENT_REJECTED);
        assertThat(message.tutorId()).isEqualTo(tutorId);
        assertThat(message.studentId()).isEqualTo(studentId);
        assertThat(message.lectureTitle()).isEqualTo(lectureTitle);

        then(pendingLectureEnrollmentRepository).should().delete(pending);
    }

    @Test
    void 다른_튜터의_토큰으로_수락하면_소유권_예외가_발생한다() {
        // given — 강의 소유 튜터는 100L, 토큰 식별자는 999L (Phase 3a 소유권 검증)
        long ownerTutorId = 100L;
        PendingAcceptCommand command = new PendingAcceptCommand(999L, 99L);
        PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
                .set("lectureId", 1L)
                .set("studentId", 2L)
                .sample();

        Lecture lecture = mock(Lecture.class);
        Tutor tutor = mock(Tutor.class);

        given(pendingLectureEnrollmentRepository.getById(anyLong())).willReturn(pending);
        given(lectureRepository.getByLectureId(1L)).willReturn(lecture);
        given(lecture.getTutor()).willReturn(tutor);
        given(tutor.getId()).willReturn(ownerTutorId);

        // when & then
        assertThatThrownBy(() -> lectureEnrollmentService.acceptEnrollment(command))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", LectureExceptionCode.TUTOR_UNAUTHORIZED);
    }
}
