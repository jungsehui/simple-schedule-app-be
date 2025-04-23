package com.example.simplescheduleapp.lecture.domain.entity;

import com.example.simplescheduleapp.schedule.domain.entity.Schedule;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Table(name = "lecture")
@NoArgsConstructor
@Getter
@Entity
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Tutor tutor;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureEnrollment> lectureEnrollments;
}
