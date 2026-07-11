package com.example.simplescheduleapp.lecture.special.domain;

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

@DiscriminatorValue("SPECIAL_LECTURE")
@Table(name = "special_lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class SpecialLecture extends Schedule {

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @Column(nullable = false)
    private int capacity;

    // 낙관적 락(@Version)은 부모 엔티티 Schedule에 정의되어 있다.
    // JPA 제약상 엔티티 계층(@Inheritance)에서 @Version은 root entity에만 둘 수 있다.

    public SpecialLecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Tutor tutor, int capacity) {
        super(title, startTime, endTime, memo);
        this.tutor = tutor;
        this.capacity = capacity;
    }

    public SpecialLectureEnrollment enroll(Student student) {
        return new SpecialLectureEnrollment(this, student);
    }

    public void update(Tutor tutor, Schedule schedule, int capacity) {
        validateTutorAuthority(tutor);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    private void validateTutorAuthority(Tutor tutor) {
        if (!this.tutor.getId().equals(tutor.getId())) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }
}
