package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.consultation.domain.Consultation;
import com.example.simplescheduleapp.consultation.domain.ConsultationAttendee;

import java.util.List;

/**
 * 순수 도메인 {@code Consultation} ↔ JPA {@code ConsultationEntity} 변환 (ADR-0004).
 *
 * <p>참석자는 애그리게잇 내부 합성이므로 루트와 함께 매핑하며, 엔티티 측 양방향 연관의
 * 주인을 {@code addAttendee}로 설정한다. <b>version 왕복이 필수다.</b>
 */
final class ConsultationMapper {

    private ConsultationMapper() {
    }

    static Consultation toDomain(ConsultationEntity entity) {
        List<ConsultationAttendee> attendees = entity.getConsultationAttendees().stream()
                .map(a -> ConsultationAttendee.reconstitute(a.getId(), a.getParentId()))
                .toList();
        return Consultation.reconstitute(
                entity.getId(),
                entity.getVersion(),   // ← 낙관적 락 왕복 (엔티티 → 도메인)
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getMemo(),
                entity.getTutorId(),
                attendees);
    }

    static ConsultationEntity toEntity(Consultation domain) {
        ConsultationEntity entity = new ConsultationEntity(
                domain.getId(),
                domain.getVersion(),   // ← 낙관적 락 왕복 (도메인 → 엔티티)
                domain.getTitle(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getMemo(),
                domain.getTutorId());
        domain.getConsultationAttendees().forEach(a ->
                entity.addAttendee(new ConsultationAttendeeEntity(a.getId(), a.getParentId())));
        return entity;
    }
}
