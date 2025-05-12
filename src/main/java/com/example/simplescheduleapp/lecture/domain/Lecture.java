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

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<LectureEnrollment> lectureEnrollments = new ArrayList<>();

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

    public LectureEnrollment enroll(Student student) {
        validateAlreadyEnrolled(student);
        validateCapacity();
        increaseEnrolledCount();
        LectureEnrollment lectureEnrollment = new LectureEnrollment(this, student);
        this.lectureEnrollments.add(lectureEnrollment);
        return lectureEnrollment;
    }

    public void update(Tutor tutor, Schedule schedule, int capacity) {
        validateTutorAuthority(tutor);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    public void cancel(LectureEnrollment lectureEnrollment) {
        lectureEnrollments.removeIf(le -> le.getId().equals(lectureEnrollment.getId()));
        decreaseEnrolledCount();
    }

    private void increaseEnrolledCount() {
        this.enrolledCount++;
    }

    private void decreaseEnrolledCount() {
        if (enrolledCount > 0) {
            this.enrolledCount--;
        }
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

    // 강의 제목 중복 에러 처리
    // 강의 콘텐츠 관리
    // 정원과 모집 상태에 따라 수강 신청
    // 수강생과 수강 대기자, 리뷰어 관리
    // 강의는 미션과 상품의 단위
}
