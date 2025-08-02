package com.example.simplescheduleapp.consultation.domain;

import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@DiscriminatorValue("CONSULTATION")
@Table(name = "consultation")
@NoArgsConstructor
@Getter
@Entity
public class Consultation extends Schedule {

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationAttendee> consultationAttendees;

    public Consultation(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Tutor tutor) {
        super(title, startTime, endTime, memo);
        this.tutor = tutor;
    }
}
