package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "UPDATE pending_lecture_enrollment SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "pending_lecture_enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PendingLectureEnrollment extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lectureId;
    private Long studentId;
    private boolean accepted = false;
    private boolean permitted = false;

    public PendingLectureEnrollment(Long lectureId, Long studentId) {
        this.lectureId = lectureId;
        this.studentId = studentId;
    }

    public void accept() {
        this.accepted = true;
        this.permitted = true;
    }

    public void reject() {
        this.accepted = true;
    }
}
