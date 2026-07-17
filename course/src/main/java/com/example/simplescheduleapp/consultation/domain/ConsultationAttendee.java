package com.example.simplescheduleapp.consultation.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상담 참석자 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>Consultation 애그리게잇의 자식(내부 합성). 순수 도메인에서는 루트로의 역참조를 갖지 않는다
 * (역참조는 JPA 양방향 매핑의 산물이므로 {@code ConsultationAttendeeEntity}가 담당).
 * 애그리게잇 간 참조(Parent)는 ID로 한다(Phase A).
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ConsultationAttendee {

    private Long id;
    private Long parentId;

    public ConsultationAttendee(Long parentId) {
        this.parentId = parentId;
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static ConsultationAttendee reconstitute(Long id, Long parentId) {
        ConsultationAttendee attendee = new ConsultationAttendee(parentId);
        attendee.id = id;
        return attendee;
    }
}
