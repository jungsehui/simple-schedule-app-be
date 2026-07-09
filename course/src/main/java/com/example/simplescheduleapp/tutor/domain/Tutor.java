package com.example.simplescheduleapp.tutor.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("TUTOR")
@Table(name = "tutor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Tutor extends Member {

    @Column(name = "career_period", nullable = false)
    private int careerPeriod;

    public Tutor(String username, String password, String name, int age, String phoneNumber, int careerPeriod) {
        super(username, password, name, age, phoneNumber);
        this.careerPeriod = careerPeriod;
    }

    @Override
    public Role getRole() {
        return Role.TUTOR;
    }
}
