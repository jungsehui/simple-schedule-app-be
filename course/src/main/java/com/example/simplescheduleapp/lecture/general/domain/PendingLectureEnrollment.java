package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.domain.BaseDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강신청 대기(pending)는 하드 삭제한다. uk_pending_lecture_student(lecture_id, student_id)가
 * deleted_date를 포함하지 않으므로, 소프트 삭제하면 취소/거절 뒤 같은 조합의 재신청 INSERT가
 * 남아 있는 행과 충돌한다(500). 확정된 수강 이력은 LectureEnrollment가 갖고, 대기 행은
 * 수락/거절/취소 시 항상 제거되는 일시 상태이므로 삭제 이력을 보존할 이유가 없다.
 */
@Table(
        name = "pending_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pending_lecture_student", columnNames = {"lecture_id", "student_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PendingLectureEnrollment extends BaseDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lectureId;
    private Long studentId;
    private boolean permitted;

    public PendingLectureEnrollment(Long lectureId, Long studentId) {
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.permitted = false;
    }

    public void accept() {
        this.permitted = true;
    }

    public void reject() {
        this.permitted = false;
    }
}
