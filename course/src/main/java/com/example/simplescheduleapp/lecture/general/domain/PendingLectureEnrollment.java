package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE pending_lecture_enrollment SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "pending_lecture_enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PendingLectureEnrollment extends SoftDeletedDomain {

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
