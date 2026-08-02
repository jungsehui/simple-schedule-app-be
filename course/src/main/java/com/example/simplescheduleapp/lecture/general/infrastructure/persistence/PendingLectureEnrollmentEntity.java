package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.common.persistence.BaseDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code PendingLectureEnrollment} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p><b>하드 삭제한다(소프트 삭제 금지).</b> uk_pending_lecture_student(lecture_id, student_id)가
 * deleted_date를 포함하지 않으므로, 소프트 삭제하면 취소/거절 뒤 같은 조합의 재신청 INSERT가
 * 남아 있는 행과 충돌한다(500 — 실서버에서 발생, main {@code 963379f}). 대기 행은 수락/거절/취소
 * 시 항상 제거되는 일시 상태이고 확정 이력은 LectureEnrollment가 담당하므로 삭제 이력을 보존할
 * 이유가 없다. {@code PendingLectureEnrollmentRepositoryTest}가 재신청 회귀를 가드한다.
 */
@Table(
        name = "pending_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pending_lecture_student", columnNames = {"lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class PendingLectureEnrollmentEntity extends BaseDomain {

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
