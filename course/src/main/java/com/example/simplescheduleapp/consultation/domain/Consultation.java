package com.example.simplescheduleapp.consultation.domain;

import com.example.simplescheduleapp.schedule.domain.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@DiscriminatorValue("CONSULTATION")
@Table(name = "consultation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Consultation extends Schedule {

    // 애그리게잇 간 참조는 ID로 한다(DDD). @Column 미사용 — 네이밍 전략이 tutorId→tutor_id 매핑. (ADR-0004 Phase A)
    private Long tutorId;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationAttendee> consultationAttendees;

    public Consultation(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Long tutorId) {
        super(title, startTime, endTime, memo);
        this.tutorId = tutorId;
    }
}
