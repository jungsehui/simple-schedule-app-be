package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DiscriminatorValue("LECTURE")
@Table(name = "lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Lecture extends Schedule {

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int enrolledCount = 0;

    public Lecture(
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String memo,
            Tutor tutor,
            int capacity
    ) {
        super(title, startTime, endTime, memo);
        this.tutor = tutor;
        this.capacity = capacity;
    }

    public void update(Tutor tutor, Schedule schedule, int capacity) {
        validateTutorAuthority(tutor);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    public void increaseEnrolledCount() {
        validateCapacity();
        this.enrolledCount++;
    }

    public void decreaseEnrolledCount() {
        if (enrolledCount > 0) {
            this.enrolledCount--;
        }
    }

    private void validateTutorAuthority(Tutor tutor) {
        if (!this.tutor.getId().equals(tutor.getId())) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }

    private void validateCapacity() {
        if (enrolledCount >= capacity) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }
}
