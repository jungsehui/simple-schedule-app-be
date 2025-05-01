package com.example.simplescheduleapp.parent.domain.entity;

import com.example.simplescheduleapp.consultation.domain.entity.ConsultationAttendee;
import com.example.simplescheduleapp.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("PARENT")
@Table(name = "parent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Parent extends Member {

    @Column(name = "children_number", nullable = false)
    private int childrenNumber;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationAttendee> consultationAttendees;

    public Parent(String username, String password, String name, int age, String phoneNumber, int childrenNumber) {
        super(username, password, name, age, phoneNumber);
        this.childrenNumber = childrenNumber;
    }
}
