package com.example.simplescheduleapp.kafka.infra.consumer;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaIdempotencyFilter;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaMessageConsumeHistory;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaMessageProcessConsumeHistoryRepository;
import com.example.simplescheduleapp.common.kafka.consumer.idempotency.IdempotencyService;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetter;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("KafkaIdempotencyFilter 은(는)")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = NotificationApplication.class)
class KafkaIdempotencyFilterTest extends ApplicationTest {

    @Autowired
    private KafkaIdempotencyFilter kafkaIdempotencyFilter;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaMessageProcessConsumeHistoryRepository kafkaMessageProcessConsumeHistoryRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Test
    void 최초_메시지는_처리한다() {
        // given
        ConsumerRecord record = new ConsumerRecord("TEST_TOPIC", 1, 1L, "", new KafkaLectureEventMessage(1L, "test-uuid", 1L));

        // when
        boolean skip = kafkaIdempotencyFilter.filter(record);

        // then
        assertThat(skip).isFalse();
    }

    @Test
    void 중복_메시지_는_처리하지_않는다() {
        // given
        kafkaMessageProcessConsumeHistoryRepository.save(new KafkaMessageConsumeHistory("test-uuid", "TEST_TOPIC"));
        ConsumerRecord record = new ConsumerRecord("TEST_TOPIC", 1, 1L, "", new KafkaLectureEventMessage(1L, "test-uuid", 1L));

        // when
        boolean skip = kafkaIdempotencyFilter.filter(record);

        // then
        assertThat(skip).isTrue();
    }

    @Test
    void 예외_메시지도_처리하지_않는다() {
        // given
        deadLetterRepository.save(new DeadLetter("test-uuid", "fail", false));
        kafkaMessageProcessConsumeHistoryRepository.save(new KafkaMessageConsumeHistory("test-uuid", "TEST_TOPIC"));
        ConsumerRecord record = new ConsumerRecord("TEST_TOPIC", 1, 1L, "", new KafkaLectureEventMessage(1L, "test-uuid", 1L));

        // when
        boolean skip = kafkaIdempotencyFilter.filter(record);

        // then
        assertThat(skip).isTrue();
    }
}
