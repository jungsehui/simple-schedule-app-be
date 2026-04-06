package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private int enrolledCount;

    public Lecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Tutor tutor, int capacity) {
        super(title, startTime, endTime, memo);
        this.tutor = tutor;
        this.capacity = capacity;
        this.enrolledCount = 0;
    }

    public LectureEnrollment enroll(Student student) {
        increaseEnrolledCount();
        return new LectureEnrollment(this, student);
    }

    public void update(Tutor tutor, Schedule schedule, int capacity) {
        validateTutorAuthority(tutor);
        validateCapacityNotBelowEnrolled(capacity);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    private void validateCapacityNotBelowEnrolled(int newCapacity) {
        if (newCapacity < this.enrolledCount) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_BELOW_ENROLLED);
        }
    }

    public void cancel() {
        validateCanDecreaseEnrolledCount();
        this.enrolledCount--;
    }

    private void increaseEnrolledCount() {
        validateCanIncreaseEnrolledCount();
        this.enrolledCount++;
    }

    private void validateTutorAuthority(Tutor tutor) {
        if (!this.tutor.getId().equals(tutor.getId())) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }

    private void validateCanIncreaseEnrolledCount() {
        if (enrolledCount >= capacity) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    private void validateCanDecreaseEnrolledCount() {
        if (enrolledCount < 1) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_UNDER_ZERO);
        }
    }
}
