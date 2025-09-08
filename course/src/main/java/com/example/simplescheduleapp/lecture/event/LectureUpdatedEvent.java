package com.example.simplescheduleapp.lecture.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@DiscriminatorValue("LECTURE_UPDATED_EVENT")
@NoArgsConstructor(access = PROTECTED)
@Getter
@Entity
public class LectureUpdatedEvent extends DomainEvent {

    Long tutorId;
    String updateDetails; // "8/7(수) 14시 -> 15시로 변경되었습니다." 등 상세 내용

    public LectureUpdatedEvent(
            Long targetDomainId,
            Long tutorId,
            String updateDetails
    ) {
        super(targetDomainId);
        this.tutorId = tutorId;
        this.updateDetails = updateDetails;
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_UPDATED_TOPIC;
    }
}
