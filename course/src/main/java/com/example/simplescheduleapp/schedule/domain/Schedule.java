package com.example.simplescheduleapp.schedule.domain;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE schedule SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Schedule extends SoftDeletedDomain {

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
        validatePastTime(startTime, endTime);
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }

    protected void updateSchedule(Schedule schedule) {
        this.title = schedule.title;
        this.startTime = schedule.startTime;
        this.endTime = schedule.endTime;
        this.memo = schedule.memo;
    }

    private void validatePastTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ApplicationException(LectureExceptionCode.INVALID_LECTURE_TIME_PAST);
        }
    }
}
