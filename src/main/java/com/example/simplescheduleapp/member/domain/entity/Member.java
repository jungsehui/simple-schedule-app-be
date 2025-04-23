package com.example.simplescheduleappback.member.domain.entity;

import com.example.simplescheduleappback.common.entity.SoftDeletedEntity;
import com.example.simplescheduleappback.common.exception.ApplicationException;
import com.example.simplescheduleappback.member.exception.MemberExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Member extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Embedded
    private Password password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    public Member(String username, String password, String name, int age, String phoneNumber) {
        this.username = username;
        this.password = Password.hashPassword(password);
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
    }

    public void login(String plainPassword) {
        boolean same = this.password.match(plainPassword);
        if (!same) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD);
        }
    }
}
