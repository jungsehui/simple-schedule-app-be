package com.example.simplescheduleapp.lecture.special.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.schedule.domain.Schedule;
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

    // 애그리게잇 간 참조는 ID로 한다(DDD). @Column 미사용 — 네이밍 전략이 tutorId→tutor_id 매핑. (ADR-0004 Phase A)
    private Long tutorId;

    @Column(nullable = false)
    private int capacity;

    // 낙관적 락(@Version)은 부모 엔티티 Schedule에 정의되어 있다.
    // JPA 제약상 엔티티 계층(@Inheritance)에서 @Version은 root entity에만 둘 수 있다.

    public SpecialLecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Long tutorId, int capacity) {
        super(title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.capacity = capacity;
    }

    public SpecialLectureEnrollment enroll(Long studentId) {
        return new SpecialLectureEnrollment(getId(), studentId);
    }

    public void update(Long tutorId, Schedule schedule, int capacity) {
        validateTutorAuthority(tutorId);
        updateSchedule(schedule);
        this.capacity = capacity;
    }

    private void validateTutorAuthority(Long tutorId) {
        if (!this.tutorId.equals(tutorId)) {
            throw new ApplicationException(LectureExceptionCode.TUTOR_UNAUTHORIZED);
        }
    }
}
