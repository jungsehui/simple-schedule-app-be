package com.example.simplescheduleappback.consultation.domain.entity;

import com.example.simplescheduleappback.parent.domain.entity.Parent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "consultation_attendee")
@NoArgsConstructor
@Getter
@Entity
public class ConsultationAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consultaition_id")
    private Consultation consultation;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Parent parent;
}
