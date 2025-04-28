package com.example.simplescheduleapp.student.domain.entity;

import com.example.simplescheduleapp.lecture.domain.entity.LectureEnrollment;
import com.example.simplescheduleapp.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("STUDENT")
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Student extends Member {

    @Column(name = "school", nullable = false)
    private String school;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureEnrollment> lectureEnrollments;

    public Student(String username, String password, String name, int age, String phoneNumber, String school) {
        super(username, password, name, age, phoneNumber);
        this.school = school;
    }
}
