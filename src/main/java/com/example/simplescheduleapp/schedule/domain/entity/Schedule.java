package com.example.simplescheduleapp.schedule.domain.entity;

import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
import com.example.simplescheduleapp.consultation.domain.entity.Consultation;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "UPDATE schedule SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Schedule extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "memo")
    private String memo;

    public Schedule(String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }

    protected void updateSchedule(String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }
}
