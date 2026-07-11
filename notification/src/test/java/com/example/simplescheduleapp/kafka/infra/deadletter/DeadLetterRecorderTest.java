package com.example.simplescheduleapp.kafka.infra.deadletter;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
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
        KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                testUuid, LectureEventType.LECTURE_UPDATED, 1L, 1L, 1L, "테스트 강의", "테스트 상세");
        ConsumerRecord record = new ConsumerRecord(testTopic, 1, 1L, "", message);

        // when
        deadLetterRecorder.accept(record, new RuntimeException("Exception"));

        // then
        assertThat(deadLetterRepository.findByUuid(testUuid)).isPresent();
    }
}
