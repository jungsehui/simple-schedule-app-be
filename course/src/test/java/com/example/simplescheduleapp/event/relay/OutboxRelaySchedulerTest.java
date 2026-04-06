package com.example.simplescheduleapp.event.relay;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.event.producer.EventProducer;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class OutboxRelaySchedulerTest extends MockTestSupport {

    @Mock
    private DomainEventRepository domainEventRepository;

    @Mock
    private EventProducer eventProducer;

    @InjectMocks
    private OutboxRelayScheduler outboxRelayScheduler;

    @Test
    void 최대_재시도_초과_이벤트는_DEAD로_전환하고_발행하지_않는다() {
        // given
        DomainEvent event = mock(DomainEvent.class);
        given(event.getRetryCount()).willReturn(5);
        given(domainEventRepository.findByStatusAndCreatedDateBefore(any(EventStatus.class), any(LocalDateTime.class)))
                .willReturn(List.of(event));
        given(domainEventRepository.findByStatus(EventStatus.PRODUCE_FAIL)).willReturn(List.of());

        // when
        outboxRelayScheduler.relayOutboxEvents();

        // then
        then(event).should().markDead();
        then(domainEventRepository).should().save(event);
        then(eventProducer).should(never()).produce(any());
    }

    @Test
    void 재시도_횟수는_발행_시도_전에_영속된다() {
        // given — 발행이 실패해도 증가한 재시도 횟수가 저장되어 MAX_RETRY 도달이 보장된다
        DomainEvent event = mock(DomainEvent.class);
        given(event.getRetryCount()).willReturn(1);
        given(domainEventRepository.findByStatusAndCreatedDateBefore(any(EventStatus.class), any(LocalDateTime.class)))
                .willReturn(List.of());
        given(domainEventRepository.findByStatus(EventStatus.PRODUCE_FAIL)).willReturn(List.of(event));
        willThrow(new RuntimeException("produce failure")).given(eventProducer).produce(event);

        // when — 발행 실패 예외는 스케줄러 내부에서 흡수되어야 한다
        outboxRelayScheduler.relayOutboxEvents();

        // then — save가 produce보다 먼저 호출됨
        InOrder order = inOrder(domainEventRepository, eventProducer);
        order.verify(domainEventRepository).save(event);
        order.verify(eventProducer).produce(event);
        then(event).should().incrementRetryCount();
    }

    @Test
    void 발행_성공_시_재시도_횟수_영속_후_발행된다() {
        // given
        DomainEvent event = mock(DomainEvent.class);
        given(event.getRetryCount()).willReturn(0);
        given(domainEventRepository.findByStatusAndCreatedDateBefore(any(EventStatus.class), any(LocalDateTime.class)))
                .willReturn(List.of(event));
        given(domainEventRepository.findByStatus(EventStatus.PRODUCE_FAIL)).willReturn(List.of());

        // when
        outboxRelayScheduler.relayOutboxEvents();

        // then
        then(event).should().incrementRetryCount();
        then(domainEventRepository).should().save(event);
        then(eventProducer).should().produce(event);
        then(event).should(never()).markDead();
    }
}
