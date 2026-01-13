package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
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
    public String getTopic() {
        return KafkaTopics.COURSE_EVENT_TOPIC;
    }
}
