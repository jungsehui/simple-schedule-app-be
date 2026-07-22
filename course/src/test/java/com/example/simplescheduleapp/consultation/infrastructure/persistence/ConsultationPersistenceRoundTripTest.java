package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.consultation.domain.Consultation;
import com.example.simplescheduleapp.consultation.domain.ConsultationAttendee;
import com.example.simplescheduleapp.consultation.domain.ConsultationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultation 애그리게잇(루트 + 참석자 합성)이 영속 매퍼를 거쳐 값 손실 없이 왕복하는지 검증한다
 * (ADR-0004).
 *
 * <p>참석자 FK 컬럼은 과거 오타({@code consultaition_id})를 정정한 직후라(V2 마이그레이션),
 * 엔티티 매핑과 스키마가 정합함을 실제 INSERT/SELECT로 실증하는 것이 목적이다.
 * {@code flush + clear}로 1차 캐시를 비우지 않으면 같은 트랜잭션의 findById가 캐시를 돌려줘
 * FK 조인 컬럼이 한 번도 SQL을 타지 않는다 — 그러면 이 테스트는 아무것도 가드하지 못한다.
 */
@DisplayName("Consultation 영속 왕복 은(는)")
@Transactional
@SpringBootTest
class ConsultationPersistenceRoundTripTest {

    @Autowired
    ConsultationRepository consultationRepository;

    @Autowired
    EntityManager entityManager;

    @DisplayName("참석자 합성이 FK 컬럼(consultation_id)을 통해 값 손실 없이 왕복한다")
    @Test
    void attendeesRoundTripThroughFkColumn() {
        // given — 참석자 2명, parentId는 서로 다른 값(뒤바뀜·유실 검출용)
        Consultation consultation = new Consultation(
                "왕복 검증 상담",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 11, 0),
                "메모",
                300L);
        consultation.addAttendee(new ConsultationAttendee(501L));
        consultation.addAttendee(new ConsultationAttendee(502L));

        Consultation saved = consultationRepository.save(consultation);

        entityManager.flush();
        entityManager.clear();

        // when — 실제 SELECT(FK 조인 컬럼 사용)
        Consultation found = consultationRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getTutorId()).isEqualTo(300L);
        assertThat(found.getConsultationAttendees())
                .extracting(ConsultationAttendee::getParentId)
                .containsExactlyInAnyOrder(501L, 502L);
        assertThat(found.getConsultationAttendees())
                .allSatisfy(a -> assertThat(a.getId()).isNotNull());
    }
}
