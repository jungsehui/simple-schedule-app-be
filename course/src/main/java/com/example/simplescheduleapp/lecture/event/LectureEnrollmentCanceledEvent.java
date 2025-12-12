package com.example.simplescheduleapp.lecture.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("ENROLLMENT_CANCELED")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureEnrollmentCanceledEvent extends DomainEvent {

    private Long studentId;      // 취소한 학생 ID
    private Long tutorId;        // 알림 받을 강사 ID
    private String lectureTitle;

    // 취소 시점에는 Student 엔티티 전체보다는 ID만 넘어오는 경우가 많아 ID로 받도록 설계
    public LectureEnrollmentCanceledEvent(Lecture lecture, Long studentId) {
        super(lecture.getId()); // targetDomainId = lectureId

        this.studentId = studentId;
        this.tutorId = lecture.getTutor().getId();
        this.lectureTitle = lecture.getTitle();
    }

    @Override
    public KafkaLectureEventMessage toMessage() {
        return KafkaLectureEventMessage.create(
                this.getUuid(),
                LectureEventType.ENROLLMENT_CANCELED,
                this.getTargetDomainId(), // lectureId
                this.studentId,
                this.tutorId,
                this.lectureTitle,
                "학생이 수강 신청을 취소하였습니다 .."
        );
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC;
    }
}
