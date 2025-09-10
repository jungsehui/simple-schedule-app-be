package com.example.simplescheduleapp.special.domain;

import com.example.simplescheduleapp.student.domain.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "special_lecture_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_special_lecture_student", columnNames = {"special_lecture_id", "student_id"})
        }
)
@NoArgsConstructor
@Getter
@Entity
public class SpecialLectureEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "special_lecture_id", nullable = false)
    private SpecialLecture specialLecture;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    public SpecialLectureEnrollment(SpecialLecture specialLecture, Student student) {
        this.specialLecture = specialLecture;
        this.student = student;
    }
}
