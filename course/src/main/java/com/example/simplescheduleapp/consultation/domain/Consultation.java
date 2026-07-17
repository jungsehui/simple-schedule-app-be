package com.example.simplescheduleapp.consultation.domain;

import com.example.simplescheduleapp.schedule.domain.Schedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 상담 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 매핑은 {@code infrastructure/persistence}의
 * {@code ConsultationEntity}가 담당한다. 애그리게잇 간 참조는 ID(tutorId)로 하고,
 * 참석자는 애그리게잇 내부 합성(루트가 자식을 소유)이므로 객체로 보유한다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Consultation extends Schedule {

    private Long tutorId;
    private List<ConsultationAttendee> consultationAttendees = new ArrayList<>();

    public Consultation(String title, LocalDateTime startTime, LocalDateTime endTime, String memo, Long tutorId) {
        super(title, startTime, endTime, memo);
        this.tutorId = tutorId;
    }

    private Consultation(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime,
                         String memo, Long tutorId, List<ConsultationAttendee> consultationAttendees) {
        super(id, version, title, startTime, endTime, memo);
        this.tutorId = tutorId;
        this.consultationAttendees = new ArrayList<>(consultationAttendees);
    }

    /** DB 복원용 재구성 팩토리 — 매퍼 전용. */
    public static Consultation reconstitute(Long id, Long version, String title, LocalDateTime startTime,
                                            LocalDateTime endTime, String memo, Long tutorId,
                                            List<ConsultationAttendee> consultationAttendees) {
        return new Consultation(id, version, title, startTime, endTime, memo, tutorId, consultationAttendees);
    }

    /** 자식 컬렉션은 루트를 통해서만 변경한다(애그리게잇 불변식 보호). */
    public List<ConsultationAttendee> getConsultationAttendees() {
        return Collections.unmodifiableList(consultationAttendees);
    }

    public void addAttendee(ConsultationAttendee attendee) {
        this.consultationAttendees.add(attendee);
    }
}
