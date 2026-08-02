package com.example.simplescheduleapp.kafka.event.outbox;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.kafka.event.mock.TestDomainEvent;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 아웃박스 갱신 시 감사 컬럼이 보존되는지 검증한다 (ADR-0004).
 *
 * <p><b>왜 이 테스트가 필요한가.</b> 도메인 순수화 이후 리포지토리 어댑터의 {@code save()}는 매퍼로
 * <em>새 엔티티 인스턴스</em>를 만들어 넘긴다. 순수 도메인은 {@code createdDate}를 들고 있지 않으므로
 * 그 인스턴스의 감사 필드는 null이고, id가 있으면 Spring Data는 {@code merge()}를 호출한다.
 * merge는 detached 인스턴스의 필드를 그대로 복사하므로 {@code created_date}가 NULL로 덮어써질 수 있다.
 *
 * <p><b>왜 치명적인가.</b> {@code OutboxRelayScheduler}는
 * {@code findByStatusAndCreatedDateBefore(INIT, threshold)}로 지연된 이벤트를 찾는다. SQL에서
 * NULL 비교는 UNKNOWN이라 {@code created_date IS NULL}인 행은 이 조건에 <em>영원히</em> 걸리지 않는다.
 * 즉 발행에 실패한 이벤트가 릴레이의 시야에서 조용히 사라진다 — 전달 보증이 깨지는데 예외는 없다.
 *
 * <p>도메인 모델을 거치지 않고 DB 컬럼을 직접 읽는 이유가 여기에 있다. 순수 도메인에는 감사 필드가
 * 없으므로 도메인 API로는 이 손실을 관측할 수 없다.
 */
@DisplayName("아웃박스 감사 컬럼 보존 은(는)")
@SpringBootTest(classes = NotificationApplication.class)
class OutboxAuditingPreservationTest extends ApplicationTest {

    @Autowired
    DomainEventRepository domainEventRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DisplayName("이미 저장된 이벤트를 다시 저장해도 created_date가 NULL로 덮어써지지 않는다")
    @Test
    void preservesCreatedDateOnUpdate() {
        // given — 최초 저장으로 created_date가 채워진다
        DomainEvent saved = domainEventRepository.save(new TestDomainEvent(1L));
        Long id = saved.getId();
        assertThat(readCreatedDate(id))
                .as("최초 저장 시 created_date가 채워져야 한다")
                .isNotNull();

        // when — 발행 성공/재시도 경로가 하는 그대로: 상태를 바꾸고 다시 저장
        saved.produceSuccess();
        domainEventRepository.save(saved);

        // then — 갱신 후에도 created_date는 살아 있어야 한다
        assertThat(readCreatedDate(id))
                .as("갱신이 created_date를 NULL로 만들면 릴레이가 이 이벤트를 영영 찾지 못한다")
                .isNotNull();
    }

    private Object readCreatedDate(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT created_date FROM domain_event WHERE id = ?", Object.class, id);
    }
}
