package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("LECTURE_UPDATED")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureUpdatedEvent extends DomainEvent {

    private Long tutorId;
    private String lectureTitle;
    private String updatedDetails; // 수정된 내용 요약

    public LectureUpdatedEvent(Lecture lecture, String updatedDetails) {
        // Target: 여기서는 Lecture ID를 타겟으로 잡음 (1:N 전파를 위해)
        // 컨슈머가 이 ID를 보고 수강생 목록을 조회해야 함
        super(lecture.getId());

        this.tutorId = lecture.getTutorId();
        this.lectureTitle = lecture.getTitle();
        this.updatedDetails = updatedDetails;
    }

    @Override
    public String getTopic() {
        return KafkaTopics.COURSE_EVENT_TOPIC;
    }
}
