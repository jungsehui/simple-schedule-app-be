package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.infrastructure.persistence.ScheduleEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code SpecialLecture} 도메인의 JPA 영속 모델 (ADR-0004). JOINED 상속의 자식.
 */
@DiscriminatorValue("SPECIAL_LECTURE")
@Table(name = "special_lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class SpecialLectureEntity extends ScheduleEntity {

    @Column(name = "tutor_id")
    private Long tutorId;

    @Column(nullable = false)
    private int capacity;

    public SpecialLectureEntity(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                                String memo, Long tutorId, int capacity) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
    }
}
