package com.example.simplescheduleapp.parent.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("PARENT")
@Table(name = "parent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Parent extends Member {

    @Column(name = "children_number", nullable = false)
    private int childrenNumber;

    public Parent(String username, String password, String name, int age, String phoneNumber, int childrenNumber) {
        super(username, password, name, age, phoneNumber);
        this.childrenNumber = childrenNumber;
    }

    @Override
    public Role getRole() {
        return Role.PARENT;
    }
}
