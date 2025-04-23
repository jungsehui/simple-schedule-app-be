package com.example.simplescheduleapp.lecture.domain.entity;

import com.example.simplescheduleapp.student.domain.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "lecture_enrollment")
@NoArgsConstructor
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
}
