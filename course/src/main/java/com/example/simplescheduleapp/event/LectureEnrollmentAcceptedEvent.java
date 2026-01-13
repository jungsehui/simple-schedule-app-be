package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
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
    public String getTopic() {
        return KafkaTopics.COURSE_EVENT_TOPIC;
    }
}
