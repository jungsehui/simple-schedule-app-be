package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code ConsultationAttendee} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>Consultation 애그리게잇 내부 합성의 자식 — 루트 역참조는 JPA 양방향 매핑용이다.
 * FK 컬럼은 과거 운영 스키마에 {@code consultaition_id}로 오타가 나 있었고,
 * {@code V2__fix_consultation_attendee_fk_typo.sql}이 {@code consultation_id}로 정정했다.
 */
@Table(name = "consultation_attendee")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class ConsultationAttendeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consultation_id")
    private ConsultationEntity consultation;

    @Column(name = "member_id")
    private Long parentId;

    public ConsultationAttendeeEntity(Long id, Long parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    void assignConsultation(ConsultationEntity consultation) {
        this.consultation = consultation;
    }
}
