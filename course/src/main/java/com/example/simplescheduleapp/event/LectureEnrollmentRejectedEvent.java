package com.example.simplescheduleapp.lecture.general.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@DiscriminatorValue("ENROLLMENT_REJECTED")
@NoArgsConstructor(access = PROTECTED)
@Getter
@Entity
public class LectureEnrollmentRejectedEvent extends DomainEvent {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentRejectedEvent(Lecture lecture, Student student) {
        super(lecture.getId()); // targetDomainId = lectureId

        this.studentId = student.getId();
        this.tutorId = lecture.getTutor().getId();
        this.lectureTitle = lecture.getTitle();
    }

    @Override
    public KafkaLectureEventMessage toMessage() {
        return KafkaLectureEventMessage.create(
                this.getUuid(),
                LectureEventType.ENROLLMENT_REJECTED,
                this.getTargetDomainId(), // lectureId
                this.studentId,
                this.tutorId,
                this.lectureTitle,
                "수강 신청이 거절되었습니다 .."
        );
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC;
    }
}
