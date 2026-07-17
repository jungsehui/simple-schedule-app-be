package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code LectureEnrollment} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>Phase A에서 도메인이 ID 참조로 바뀌며 잠시 유보했던 NOT NULL 제약을 여기서 복원한다.
 */
@Table(
        name = "lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lecture_student", columnNames = {"lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureEnrollmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    public LectureEnrollmentEntity(Long id, Long lectureId, Long studentId) {
        this.id = id;
        this.lectureId = lectureId;
        this.studentId = studentId;
    }
}
