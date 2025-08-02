package com.example.simplescheduleapp.student.domain;

import com.example.simplescheduleapp.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("STUDENT")
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Student extends Member {

    @Column(name = "school", nullable = false)
    private String school;

    public Student(String username, String password, String name, int age, String phoneNumber, String school) {
        super(username, password, name, age, phoneNumber);
        this.school = school;
    }
}
