package com.example.simplescheduleapp.kafka.infra.deadletter;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRecorder;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeadLetterRecorder 은(는)")
@Transactional
@SpringBootTest(classes = NotificationApplication.class)
class DeadLetterRecorderTest extends ApplicationTest {

    @Autowired
    private DeadLetterRecorder deadLetterRecorder;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @DisplayName("카프카 컨슘 중 예외 발생 시 Dead Letter 저장")
    @Test
    void recordDeadLetterWhenExceptionOccurs() {
        // given
        String testTopic = "TEST_TOPIC";
        String testUuid = "test-uuid";
        ConsumerRecord record = new ConsumerRecord(testTopic, 1, 1L, "", new KafkaDomainEventMessage(1L, testUuid, 1L));

        // when
        deadLetterRecorder.accept(record, new RuntimeException("Exception"));

        // then
        assertThat(deadLetterRepository.findByUuid(testUuid)).isPresent();
    }
}
