package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code SpecialLectureEnrollment} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>Phase A에서 도메인이 ID 참조로 바뀌며 잠시 유보했던 NOT NULL 제약을 여기서 복원한다.
 * 유니크 제약(uk_special_lecture_student)은 선착순 중복 신청 방어의 최종 방어선이다.
 */
@Table(
        name = "special_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_special_lecture_student", columnNames = {"special_lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class SpecialLectureEnrollmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "special_lecture_id", nullable = false)
    private Long specialLectureId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    public SpecialLectureEnrollmentEntity(Long id, Long specialLectureId, Long studentId) {
        this.id = id;
        this.specialLectureId = specialLectureId;
        this.studentId = studentId;
    }
}
