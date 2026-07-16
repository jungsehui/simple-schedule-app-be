package com.example.simplescheduleapp.consultation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "consultation_attendee")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class ConsultationAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 애그리게잇 내부 합성(루트 Consultation의 자식) — 객체 참조 유지가 DDD상 정당하며
    // Consultation.@OneToMany(mappedBy="consultation")가 이 필드를 요구한다.
    @ManyToOne
    @JoinColumn(name = "consultaition_id")
    private Consultation consultation;

    // 애그리게잇 간 참조(Parent)는 ID로 한다(DDD). 컬럼명이 member_id라 네이밍 전략으로
    // 매핑되지 않아 @Column 명시가 불가피 — Phase B에서 JPA 엔티티로 이관하며 제거된다.
    // (ADR-0004 Phase A)
    @Column(name = "member_id")
    private Long parentId;
}
