package com.example.simplescheduleapp.student.infrastructure.persistence;

import com.example.simplescheduleapp.member.infrastructure.persistence.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code Student} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>매핑은 순수화 전 {@code Student} @Entity에서 그대로 이관 — 운영 스키마 불변.
 */
@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("STUDENT")
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class StudentEntity extends MemberEntity {

    @Column(name = "school", nullable = false)
    private String school;

    public StudentEntity(Long id, String username, String password, String name, int age, String phoneNumber, String school) {
        super(id, username, password, name, age, phoneNumber);
        this.school = school;
    }
}
