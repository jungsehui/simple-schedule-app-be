package com.example.simplescheduleapp.consultation.domain;

import com.example.simplescheduleapp.parent.domain.Parent;
import jakarta.persistence.*;
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
