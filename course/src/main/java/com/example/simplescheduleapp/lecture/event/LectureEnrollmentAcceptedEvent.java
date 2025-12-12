package com.example.simplescheduleapp.lecture.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("ENROLLMENT_ACCEPTED")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureEnrollmentAcceptedEvent extends DomainEvent {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentAcceptedEvent(Lecture lecture, Student student) {
        // targetDomainId에는 '이 사건의 핵심'인 LectureId를 넣거나,
        // 혹은 Outbox 패턴 식별용으로만 쓰고 비즈니스 로직에선 무시
        super(lecture.getId());

        // 이벤트 당사자들을 모두 명시
        this.studentId = student.getId();
        this.tutorId = lecture.getTutor().getId(); // 단순 사실 관계 기록용
        this.lectureTitle = lecture.getTitle();
    }

    @Override
    public KafkaLectureEventMessage toMessage() {
        // 메시지 생성 (문구 없음 ! 데이터만 있음)
        return KafkaLectureEventMessage.create(
                this.getUuid(),
                LectureEventType.ENROLLMENT_ACCEPTED,
                this.getTargetDomainId(), // lectureId
                this.studentId,
                this.tutorId,
                this.lectureTitle,
                "수강 신청이 수락되었습니다 !"
        );
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC;
    }
}
