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

    // 낙관적 락(@Version)은 부모 엔티티 Schedule에 정의되어 있다.
    // JPA 제약상 엔티티 계층(@Inheritance)에서 @Version은 root entity에만 둘 수 있다.
    // Schedule.version으로 enrolledCount의 동시 수정(race condition)이 자동으로 감지된다.

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
        updateSchedule(schedule);
        this.capacity = capacity;
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
