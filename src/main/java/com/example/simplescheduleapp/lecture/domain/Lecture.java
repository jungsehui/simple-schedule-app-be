package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureEnrollment> lectureEnrollments;

    public Lecture(
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String memo,
            Tutor tutor,
            int capacity
    ) {
        super(title, startTime, endTime, memo);
        validatePastTime(startTime, endTime);
        this.tutor = tutor;
        this.capacity = capacity;
    }

    public LectureEnrollment enroll(Student student) {
        validateAlreadyEnrolled(student);
        validateCapacity();
        increaseEnrolledCount();
        return new LectureEnrollment(this, student);
    }

    public void updateLecture(Tutor tutor, LectureUpdateCommand command) {
        validateTutorAuthority(tutor);
        updateSchedule(command.title(), command.startTime(), command.endTime(), command.memo());
        this.capacity = command.capacity();
    }

    public void decreaseEnrolledCount() {
        if (enrolledCount > 0) {
            this.enrolledCount--;
        }
    }

    private void increaseEnrolledCount() {
        this.enrolledCount++;
    }

    private void validateTutorAuthority(Tutor tutor) {
        if (!this.tutor.getId().equals(tutor.getId())) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }

    private void validateAlreadyEnrolled(Student student) {
        boolean alreadyEnrolled = lectureEnrollments.stream()
                .anyMatch(enrollment -> enrollment.getStudent().getId().equals(student.getId()));
        if (alreadyEnrolled) {
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        }
    }

    private void validateCapacity() {
        if (enrolledCount >= capacity) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    private void validatePastTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ApplicationException(LectureExceptionCode.INVALID_LECTURE_TIME_PAST);
        }
    }

    // 강의 제목 중복 에러 처리
    // 강의 콘텐츠를 관리한다
    // 정원과 모집 상태에 따라 수강 신청을 받는다.
    // 수강생과 수강 대기자, 리뷰어 관리한다
    // 강의는 미션과 상품의 단위가 되기도 한다.
}
