package com.example.simplescheduleapp.kafka.event.producer;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.event.producer.KafkaEventProducer;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.kafka.event.mock.TestDomainEvent;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

@DisplayName("KafkaEventProducer 은(는)")
@SpringBootTest(classes = NotificationApplication.class)
class KafkaEventProducerTest extends ApplicationTest {

    @Autowired
    private KafkaEventProducer kafkaEventProducer;

    @Autowired
    private DomainEventRepository domainEventRepository;

    @MockitoBean
    private KafkaProducer<KafkaLectureEventMessage> kafkaProducer;

    @DisplayName("이벤트 발행 성공 시 이벤트의 상태를 성공으로 저장")
    @Test
    void recordEventSuccessWhenEventProducedSuccessfully() {
        // given
        TestDomainEvent topic = new TestDomainEvent(1L, "TEST_TOPIC");

        // when
        kafkaEventProducer.produce(topic);

        // then
        EventStatus state = domainEventRepository.getByUuid(topic.getUuid()).getStatus();
        assertThat(state).isEqualTo(EventStatus.PRODUCE_SUCCESS);
    }

    @DisplayName("이벤트 발행 실패 시 이벤트의 상태를 실패로 저장")
    @Test
    void recordEventFailWhenEventProducedFail() {
        // given
        TestDomainEvent topic = new TestDomainEvent(1L, "TEST_TOPIC");
        willThrow(RuntimeException.class)
                .given(kafkaProducer)
                .produce(any(), any());

        // when
        assertThatThrownBy(() -> {
            kafkaEventProducer.produce(topic);
        }).isInstanceOf(ApplicationException.class);

        // then
        EventStatus state = domainEventRepository.getByUuid(topic.getUuid()).getStatus();
        assertThat(state).isEqualTo(EventStatus.PRODUCE_FAIL);
    }
}
