package com.example.simplescheduleapp.parent.infrastructure.persistence;

import com.example.simplescheduleapp.member.infrastructure.persistence.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code Parent} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>매핑은 순수화 전 {@code Parent} @Entity에서 그대로 이관 — 운영 스키마 불변.
 */
@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("PARENT")
@Table(name = "parent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class ParentEntity extends MemberEntity {

    @Column(name = "children_number", nullable = false)
    private int childrenNumber;

    public ParentEntity(Long id, String username, String password, String name, int age, String phoneNumber, int childrenNumber) {
        super(id, username, password, name, age, phoneNumber);
        this.childrenNumber = childrenNumber;
    }
}
