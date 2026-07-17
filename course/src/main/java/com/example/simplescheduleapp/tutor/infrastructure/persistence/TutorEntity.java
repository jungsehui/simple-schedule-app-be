package com.example.simplescheduleapp.tutor.infrastructure.persistence;

import com.example.simplescheduleapp.member.infrastructure.persistence.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code Tutor} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>매핑은 순수화 전 {@code Tutor} @Entity에서 그대로 이관 — 운영 스키마 불변.
 */
@PrimaryKeyJoinColumn(name = "member_id")
@DiscriminatorValue("TUTOR")
@Table(name = "tutor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class TutorEntity extends MemberEntity {

    @Column(name = "career_period", nullable = false)
    private int careerPeriod;

    public TutorEntity(Long id, String username, String password, String name, int age, String phoneNumber, int careerPeriod) {
        super(id, username, password, name, age, phoneNumber);
        this.careerPeriod = careerPeriod;
    }
}
