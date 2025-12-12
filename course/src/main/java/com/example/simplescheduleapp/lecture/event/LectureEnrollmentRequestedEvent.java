package com.example.simplescheduleapp.lecture.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollment;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@DiscriminatorValue("ENROLLMENT_REQUESTED")
@NoArgsConstructor(access = PROTECTED)
@Getter
@Entity
public class LectureEnrollmentRequestedEvent extends DomainEvent {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentRequestedEvent(PendingLectureEnrollment pending, Lecture lecture) {
        super(lecture.getId());

        this.studentId = pending.getStudentId();
        this.tutorId = lecture.getTutor().getId();
        this.lectureTitle = lecture.getTitle();
    }

    @Override
    public KafkaLectureEventMessage toMessage() {
        return KafkaLectureEventMessage.create(
                this.getUuid(),
                LectureEventType.ENROLLMENT_REQUESTED,
                this.getTargetDomainId(), // lectureId
                this.studentId,
                this.tutorId,
                this.lectureTitle,
                "새로운 수강 신청 요청이 도착했습니다."
        );
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC;
    }
}
