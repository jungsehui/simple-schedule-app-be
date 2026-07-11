package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.student.domain.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lecture_student", columnNames = {"lecture_id", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    public LectureEnrollment(Lecture lecture, Student student) {
        this.lecture = lecture;
        this.student = student;
    }
}
