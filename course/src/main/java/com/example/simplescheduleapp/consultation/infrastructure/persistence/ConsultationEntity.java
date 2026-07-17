package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.infrastructure.persistence.ScheduleEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Consultation} 도메인의 JPA 영속 모델 (ADR-0004). JOINED 상속의 자식.
 *
 * <p>참석자는 애그리게잇 내부 합성이므로 엔티티 측에서 양방향(@OneToMany/@ManyToOne)을 유지한다.
 */
@DiscriminatorValue("CONSULTATION")
@Table(name = "consultation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class ConsultationEntity extends ScheduleEntity {

    @Column(name = "tutor_id")
    private Long tutorId;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationAttendeeEntity> consultationAttendees = new ArrayList<>();

    public ConsultationEntity(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                              String memo, Long tutorId) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
    }

    /** 양방향 연관의 주인(ConsultationAttendeeEntity.consultation)을 함께 설정한다. */
    void addAttendee(ConsultationAttendeeEntity attendee) {
        this.consultationAttendees.add(attendee);
        attendee.assignConsultation(this);
    }
}
