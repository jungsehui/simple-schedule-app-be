package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

/**
 * {@code PendingLectureEnrollment} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>감사·소프트삭제는 영속 관심사이므로 도메인이 아닌 여기서 담당한다.
 */
@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE pending_lecture_enrollment SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
        name = "pending_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pending_lecture_student", columnNames = {"lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class PendingLectureEnrollmentEntity extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private boolean permitted;

    public PendingLectureEnrollmentEntity(Long id, Long lectureId, Long studentId, boolean permitted) {
        this.id = id;
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.permitted = permitted;
    }
}
