package com.example.simplescheduleapp.notification.infrastructure;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.application.strategy.NotificationCommand;
import com.example.simplescheduleapp.notification.application.strategy.NotificationEventType;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategyFactory;
import com.example.simplescheduleapp.notification.exception.NotificationTypeExceptionCode;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 인바운드 어댑터의 Kafka DTO 변환 검증.
 *
 * <p>전략을 커맨드로 떼어낸 뒤 유일하게 남은 신규 로직이 이 변환이다.
 * {@code lectureId}, {@code studentId}, {@code tutorId}는 셋 다 {@code Long}이라
 * 자리를 바꿔 넣어도 컴파일이 통과한다. 그래서 세 값을 서로 다르게 주고 각 자리를 못박는다.
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationKafkaConsumerTest extends UnitTest {

    @InjectMocks
    private NotificationKafkaConsumer notificationKafkaConsumer;

    @Mock
    private NotificationStrategyFactory notificationStrategyFactory;

    @Mock
    private NotificationStrategy notificationStrategy;

    @Mock
    private Acknowledgment ack;

    @Test
    @DisplayName("Kafka 메시지의 각 값이 커맨드의 제 자리에 매핑된다")
    void 커맨드_필드_매핑() {
        // Long 3개(lectureId/studentId/tutorId)를 서로 다른 값으로 둬야 자리 바꿔치기가 잡힌다.
        KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                "uuid-1", LectureEventType.ENROLLMENT_ACCEPTED, 100L, 7L, 42L, "자료구조 특강", "승낙되었습니다");
        given(notificationStrategyFactory.getStrategy(any())).willReturn(notificationStrategy);

        notificationKafkaConsumer.consumeNotificationEvent(message, ack, 0);

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationStrategy).handle(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new NotificationCommand(100L, 7L, 42L, "자료구조 특강", "승낙되었습니다"));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("전송 계층 이벤트 종류 전부가 같은 이름의 유스케이스 종류로 매핑된다")
    void 이벤트_종류_매핑() {
        given(notificationStrategyFactory.getStrategy(any())).willReturn(notificationStrategy);
        ArgumentCaptor<NotificationEventType> captor = ArgumentCaptor.forClass(NotificationEventType.class);

        for (LectureEventType type : LectureEventType.values()) {
            notificationKafkaConsumer.consumeNotificationEvent(
                    KafkaLectureEventMessage.create("uuid", type, 1L, 2L, 3L, "제목", "내용"), ack, 0);
        }

        verify(notificationStrategyFactory, times(LectureEventType.values().length))
                .getStrategy(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationEventType::name)
                .containsExactly(Arrays.stream(LectureEventType.values())
                        .map(LectureEventType::name)
                        .toArray(String[]::new));
    }

    @Test
    @DisplayName("type이 null이면 전략을 타지 않고 NOTIFICATION_TYPE_NOT_FOUND로 실패한다")
    void 타입_null이면_예외() {
        KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                "uuid-null", null, 1L, 2L, 3L, "제목", "내용");

        assertThatThrownBy(() -> notificationKafkaConsumer.consumeNotificationEvent(message, ack, 0))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode())
                        .isEqualTo(NotificationTypeExceptionCode.NOTIFICATION_TYPE_NOT_FOUND));

        verifyNoInteractions(notificationStrategyFactory, notificationStrategy, ack);
    }

    @Test
    @DisplayName("NotificationEventType과 LectureEventType의 상수 개수가 같다 — 한쪽에만 추가되면 이 테스트가 잡는다")
    void 이벤트_종류_개수_대조() {
        assertThat(NotificationEventType.values()).hasSameSizeAs(LectureEventType.values());
    }
}
