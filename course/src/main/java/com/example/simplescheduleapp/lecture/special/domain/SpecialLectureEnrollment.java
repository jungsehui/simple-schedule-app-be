package com.example.simplescheduleapp.lecture.special.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "special_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_special_lecture_student", columnNames = {"special_lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class SpecialLectureEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 애그리게잇 간 참조는 ID로 한다(DDD). @Column 미사용 — 네이밍 전략이 specialLectureId→special_lecture_id,
    // studentId→student_id로 매핑(스키마 불변). NOT NULL은 Phase B에서 JPA 엔티티로 이관하며 복원.
    // (ADR-0004 Phase A)
    private Long specialLectureId;

    private Long studentId;

    public SpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        this.specialLectureId = specialLectureId;
        this.studentId = studentId;
    }
}
