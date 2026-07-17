package com.example.simplescheduleapp.kafka.event.outbox;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.outbox.EventRecorder;
import com.example.simplescheduleapp.kafka.event.mock.TestDomainEvent;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventRecorder 은(는)")
@Transactional
@SpringBootTest(classes = NotificationApplication.class)
class EventRecorderTest extends ApplicationTest {

    @Autowired
    EventRecorder eventRecorder;

    @Autowired
    DomainEventRepository domainEventRepository;

    @DisplayName("이벤트를 성공적으로 저장한다")
    @Test
    void recordEventSuccessfully() {
        // given
        DomainEvent testDomainEvent = new TestDomainEvent(1L);

        // when
        eventRecorder.record(testDomainEvent);

        // then
        Optional<DomainEvent> byId = domainEventRepository.findById(testDomainEvent.getId());
        assertThat(byId).isPresent();
        assertThat(byId.get().getStatus()).isEqualTo(EventStatus.INIT);
    }

    @DisplayName("UUID 중복으로 DataIntegrityViolationException 발생 시, UUID를 재생성하여 저장을 재시도한다.")
    @Test
    void shouldRetryWhenUuidIsDuplicated() {
        // given
        TestDomainEvent testDomainEvent = new TestDomainEvent(1L);
        String duplicatedUuid = testDomainEvent.getUuid();
        domainEventRepository.save(testDomainEvent);

        TestDomainEvent duplicatedTestDomainEvent = new TestDomainEvent(duplicatedUuid, EventStatus.INIT, 1L, "TEST_TOPIC");

        // when
        DomainEvent duplicated = eventRecorder.record(duplicatedTestDomainEvent);

        // then
        assertThat(duplicated.getUuid()).isNotEqualTo(duplicatedUuid);
    }
}
