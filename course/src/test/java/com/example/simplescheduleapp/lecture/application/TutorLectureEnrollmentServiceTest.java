package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.AcceptLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RejectLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.lecture.application.command.PendingAcceptCommand;
import com.example.simplescheduleapp.lecture.application.command.PendingRejectCommand;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationWithKafkaTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
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
    void 수강등록_수락_처리() throws JsonProcessingException {
        // given
        long tutorId = 100L;
        long studentId = 2L;
        String lectureTitle = "수학";

        PendingAcceptCommand command = new PendingAcceptCommand(99L);
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
        // 4. Kafka 메시지 소비 및 검증
        ConsumerRecords<String, String> records = waitingConsumeTopicSync(KafkaTopics.ACCEPT_LECTURE_ENROLLMENT_TOPIC);
        assertThat(records.count()).isEqualTo(1); // 1개의 메시지만 소비되었는지 확인

        ConsumerRecord<String, String> record = records.iterator().next();
        AcceptLectureEnrollmentTopicMessage message = objectMapper.readValue(record.value(), AcceptLectureEnrollmentTopicMessage.class);

        assertThat(message.senderId()).isEqualTo(tutorId);
        assertThat(message.targetId()).isEqualTo(studentId);
        assertThat(message.lectureTitle()).isEqualTo(lectureTitle);

        // 5. 기존 DB 검증 유지
        then(lectureEnrollmentRepository).should().save(enrollment);
        then(pendingLectureEnrollmentRepository).should().delete(pending);
    }

    @Test
    void 수강등록_거부_처리() throws JsonProcessingException {
        // given
        long tutorId = 100L;
        long studentId = 2L;
        String lectureTitle = "영어";

        PendingRejectCommand command = new PendingRejectCommand(88L);
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
        ConsumerRecords<String, String> records = waitingConsumeTopicSync(KafkaTopics.REJECT_LECTURE_ENROLLMENT_TOPIC);
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        RejectLectureEnrollmentTopicMessage message = objectMapper.readValue(record.value(), RejectLectureEnrollmentTopicMessage.class);

        assertThat(message.senderId()).isEqualTo(tutorId);
        assertThat(message.targetId()).isEqualTo(studentId);
        assertThat(message.lectureTitle()).isEqualTo(lectureTitle);

        then(pendingLectureEnrollmentRepository).should().delete(pending);
    }
}
