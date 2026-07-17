package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.event.LectureEnrollmentAcceptedEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentCanceledEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentRejectedEvent;
import com.example.simplescheduleapp.event.LectureEnrollmentRequestedEvent;
import com.example.simplescheduleapp.event.LectureUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * course 도메인 이벤트 5종이 영속 매퍼를 거쳐 값 손실 없이 왕복하는지 검증한다 (ADR-0004).
 *
 * <p><b>왜 필요한가.</b> 순수화로 도메인과 엔티티가 갈라지면서 둘 사이의 값 전달이 손으로 쓴
 * 매퍼에 의존하게 됐다. {@code studentId}와 {@code tutorId}는 <em>둘 다 Long</em>이라 매퍼에서
 * 서로 뒤바뀌어도 컴파일되고 타입 검사도 통과한다. 그 결과는 조용하다 — 학생에게 갈 알림이
 * 강사에게 가고 예외는 없다.
 *
 * <p>기존 아웃박스 테스트는 모두 {@code TestDomainEvent} 더블을 쓰고 이 필드들이 없어서 이 결함을
 * 구조적으로 잡을 수 없다. 그래서 프로덕션 이벤트를 리포지토리로 실제 왕복시킨다.
 *
 * <p>각 필드에 <b>서로 다른 값</b>을 주는 것이 이 테스트의 핵심이다. 같은 값을 쓰면 스왑이
 * 통과해버려 테스트가 아무것도 가드하지 못한다.
 */
@DisplayName("course 이벤트 영속 왕복 은(는)")
@SpringBootTest
class CourseEventPersistenceRoundTripTest {

    private static final Long LECTURE_ID = 100L;
    private static final Long STUDENT_ID = 200L;
    private static final Long TUTOR_ID = 300L;
    private static final String LECTURE_TITLE = "왕복 검증용 강의";

    @Autowired
    DomainEventRepository domainEventRepository;

    @DisplayName("수강신청 요청 이벤트가 학생·강사 ID를 뒤바꾸지 않고 왕복한다")
    @Test
    void requestedEventRoundTrips() {
        DomainEvent reloaded = saveAndReload(new LectureEnrollmentRequestedEvent(
                null, "uuid-requested", EventStatus.INIT, LECTURE_ID, null, 0,
                STUDENT_ID, TUTOR_ID, LECTURE_TITLE));

        assertThat(reloaded).isInstanceOf(LectureEnrollmentRequestedEvent.class);
        LectureEnrollmentRequestedEvent e = (LectureEnrollmentRequestedEvent) reloaded;
        assertThat(e.getStudentId()).as("studentId가 tutorId와 뒤바뀌면 안 된다").isEqualTo(STUDENT_ID);
        assertThat(e.getTutorId()).isEqualTo(TUTOR_ID);
        assertThat(e.getLectureTitle()).isEqualTo(LECTURE_TITLE);
        assertThat(e.getTargetDomainId()).isEqualTo(LECTURE_ID);
        assertThat(e.getTopic()).isEqualTo(KafkaTopics.COURSE_EVENT_TOPIC);
    }

    @DisplayName("수강신청 수락 이벤트가 학생·강사 ID를 뒤바꾸지 않고 왕복한다")
    @Test
    void acceptedEventRoundTrips() {
        DomainEvent reloaded = saveAndReload(new LectureEnrollmentAcceptedEvent(
                null, "uuid-accepted", EventStatus.INIT, LECTURE_ID, null, 0,
                STUDENT_ID, TUTOR_ID, LECTURE_TITLE));

        assertThat(reloaded).isInstanceOf(LectureEnrollmentAcceptedEvent.class);
        LectureEnrollmentAcceptedEvent e = (LectureEnrollmentAcceptedEvent) reloaded;
        assertThat(e.getStudentId()).as("studentId가 tutorId와 뒤바뀌면 안 된다").isEqualTo(STUDENT_ID);
        assertThat(e.getTutorId()).isEqualTo(TUTOR_ID);
        assertThat(e.getLectureTitle()).isEqualTo(LECTURE_TITLE);
    }

    @DisplayName("수강신청 거절 이벤트가 학생·강사 ID를 뒤바꾸지 않고 왕복한다")
    @Test
    void rejectedEventRoundTrips() {
        DomainEvent reloaded = saveAndReload(new LectureEnrollmentRejectedEvent(
                null, "uuid-rejected", EventStatus.INIT, LECTURE_ID, null, 0,
                STUDENT_ID, TUTOR_ID, LECTURE_TITLE));

        assertThat(reloaded).isInstanceOf(LectureEnrollmentRejectedEvent.class);
        LectureEnrollmentRejectedEvent e = (LectureEnrollmentRejectedEvent) reloaded;
        assertThat(e.getStudentId()).as("studentId가 tutorId와 뒤바뀌면 안 된다").isEqualTo(STUDENT_ID);
        assertThat(e.getTutorId()).isEqualTo(TUTOR_ID);
        assertThat(e.getLectureTitle()).isEqualTo(LECTURE_TITLE);
    }

    @DisplayName("수강신청 취소 이벤트가 학생·강사 ID를 뒤바꾸지 않고 왕복한다")
    @Test
    void canceledEventRoundTrips() {
        DomainEvent reloaded = saveAndReload(new LectureEnrollmentCanceledEvent(
                null, "uuid-canceled", EventStatus.INIT, LECTURE_ID, null, 0,
                STUDENT_ID, TUTOR_ID, LECTURE_TITLE));

        assertThat(reloaded).isInstanceOf(LectureEnrollmentCanceledEvent.class);
        LectureEnrollmentCanceledEvent e = (LectureEnrollmentCanceledEvent) reloaded;
        assertThat(e.getStudentId()).as("studentId가 tutorId와 뒤바뀌면 안 된다").isEqualTo(STUDENT_ID);
        assertThat(e.getTutorId()).isEqualTo(TUTOR_ID);
        assertThat(e.getLectureTitle()).isEqualTo(LECTURE_TITLE);
    }

    @DisplayName("강의 수정 이벤트가 상태·실패사유·재시도횟수까지 왕복한다")
    @Test
    void updatedEventRoundTripsIncludingOutboxState() {
        // 발행 실패 후 재시도 중인 상태 — 릴레이가 의존하는 필드들이 살아남는지 함께 본다
        DomainEvent reloaded = saveAndReload(new LectureUpdatedEvent(
                null, "uuid-updated", EventStatus.PRODUCE_FAIL, LECTURE_ID, "브로커 연결 실패", 3,
                TUTOR_ID, LECTURE_TITLE, "시간 변경"));

        assertThat(reloaded).isInstanceOf(LectureUpdatedEvent.class);
        LectureUpdatedEvent e = (LectureUpdatedEvent) reloaded;
        assertThat(e.getTutorId()).isEqualTo(TUTOR_ID);
        assertThat(e.getLectureTitle()).isEqualTo(LECTURE_TITLE);
        assertThat(e.getUpdatedDetails()).isEqualTo("시간 변경");
        assertThat(e.getStatus()).as("릴레이가 이 상태로 재발행 대상을 고른다").isEqualTo(EventStatus.PRODUCE_FAIL);
        assertThat(e.getFailReason()).isEqualTo("브로커 연결 실패");
        assertThat(e.getRetryCount()).as("재시도 횟수가 유실되면 MAX_RETRY 도달이 영영 안 온다").isEqualTo(3);
    }

    private DomainEvent saveAndReload(DomainEvent event) {
        Long id = domainEventRepository.save(event).getId();
        assertThat(id).as("저장 시 식별자가 부여돼야 한다").isNotNull();
        return domainEventRepository.findById(id).orElseThrow();
    }
}
