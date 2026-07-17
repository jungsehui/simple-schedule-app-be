package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.infrastructure.persistence.ScheduleEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code Lecture} 도메인의 JPA 영속 모델 (ADR-0004). JOINED 상속의 자식.
 */
@DiscriminatorValue("LECTURE")
@Table(name = "lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureEntity extends ScheduleEntity {

    @Column(name = "tutor_id")
    private Long tutorId;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int enrolledCount;

    public LectureEntity(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                         String memo, Long tutorId, int capacity, int enrolledCount) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
    }
}
