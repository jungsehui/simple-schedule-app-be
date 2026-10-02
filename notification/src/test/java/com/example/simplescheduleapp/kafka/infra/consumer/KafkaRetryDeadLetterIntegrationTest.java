package com.example.simplescheduleapp.kafka.infra.consumer;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaMessageProcessConsumeHistoryRepository;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRepository;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.fcm.infrastructure.FcmMessageSender;
import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategyFactory;
import com.example.simplescheduleapp.sse.infrastructure.redis.RedisClientManager;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 멱등성 필터와 재시도/dead letter의 상호작용 계약.
 *
 * <p>처리 기록(kafka_message_consume_history)은 처리에 성공한 뒤에만 남아야 한다. 처리 전에 기록하면
 * DefaultErrorHandler의 재시도가 필터에서 "이미 처리됨"으로 걸러져, 실패한 메시지가 재시도도
 * dead letter도 없이 사라진다.
 */
@DisplayName("Kafka 재시도와 dead letter 은(는)")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@EmbeddedKafka(topics = {KafkaTopics.COURSE_EVENT_TOPIC}, partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@SpringBootTest(classes = NotificationApplication.class)
class KafkaRetryDeadLetterIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaMessageProcessConsumeHistoryRepository consumeHistoryRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @MockitoBean
    private NotificationStrategyFactory notificationStrategyFactory;

    // 컨텍스트 기동용 외부 의존성 (NotificationIntegrationTest와 같은 구성)
    @MockitoBean
    private RedisClientManager redisClientManager;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @MockitoBean
    private FcmMessageSender fcmMessageSender;

    @MockitoBean
    private EnrolledStudentsPort enrolledStudentsPort;

    private final NotificationStrategy strategy = mock(NotificationStrategy.class);

    @BeforeEach
    void setUp() {
        given(notificationStrategyFactory.getStrategy(any())).willReturn(strategy);
    }

    @Test
    void 처리가_한_번_실패한_메시지는_재시도로_다시_처리되고_처리_기록이_남는다() {
        // given
        String uuid = "retry-" + UUID.randomUUID();
        doThrow(new RuntimeException("일시 장애"))
                .doNothing()
                .when(strategy).handle(any());

        // when
        kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message(uuid));

        // then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            verify(strategy, times(2)).handle(any());
            assertThat(consumeHistoryRepository.findByUuid(uuid)).isPresent();
        });
        assertThat(deadLetterRepository.findByUuid(uuid)).isEmpty();
    }

    @Test
    void 재시도까지_모두_실패한_메시지는_dead_letter로_남고_처리_기록은_남지_않는다() {
        // given
        String uuid = "dead-" + UUID.randomUUID();
        doThrow(new RuntimeException("계속 장애")).when(strategy).handle(any());

        // when
        kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message(uuid));

        // then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(deadLetterRepository.findByUuid(uuid)).isPresent());
        assertThat(consumeHistoryRepository.findByUuid(uuid)).isEmpty();
    }

    @Test
    void 처리에_성공한_메시지가_다시_오면_한_번만_처리한다() {
        // given
        String uuid = "dup-" + UUID.randomUUID();
        doNothing().when(strategy).handle(any());

        // when
        kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message(uuid));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(consumeHistoryRepository.findByUuid(uuid)).isPresent());
        kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message(uuid));

        // then: 두 번째 메시지가 소비될 시간을 준 뒤에도 처리는 1회
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(strategy, times(1)).handle(any()));
    }

    private static KafkaLectureEventMessage message(String uuid) {
        return KafkaLectureEventMessage.create(
                uuid, LectureEventType.LECTURE_UPDATED, 100L, null, 10L, "테스트 강의", "강의가 수정되었습니다");
    }
}
