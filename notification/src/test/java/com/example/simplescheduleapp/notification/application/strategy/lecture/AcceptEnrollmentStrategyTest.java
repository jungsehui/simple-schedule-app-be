package com.example.simplescheduleapp.notification.application.strategy.lecture;

import com.example.simplescheduleapp.notification.application.NotificationFacade;
import com.example.simplescheduleapp.notification.application.strategy.NotificationCommand;
import com.example.simplescheduleapp.notification.application.strategy.NotificationEventType;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 수강 신청 승낙 전략 단위 테스트.
 *
 * <p>이 테스트에는 {@code common.kafka} import가 하나도 없다. 그것이 이 리팩터의 목적이다.
 * 전략은 Kafka DTO가 아니라 유스케이스 소유 커맨드({@code NotificationCommand})만 받는다.
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AcceptEnrollmentStrategyTest extends UnitTest {

    @InjectMocks
    private AcceptEnrollmentStrategy acceptEnrollmentStrategy;

    @Mock
    private NotificationFacade notificationFacade;

    @Test
    @DisplayName("ENROLLMENT_ACCEPTED 이벤트를 지원한다")
    void 지원_이벤트_종류() {
        assertThat(acceptEnrollmentStrategy.getSupportType()).isEqualTo(NotificationEventType.ENROLLMENT_ACCEPTED);
        assertThat(acceptEnrollmentStrategy.supports(NotificationEventType.ENROLLMENT_ACCEPTED)).isTrue();
        assertThat(acceptEnrollmentStrategy.supports(NotificationEventType.ENROLLMENT_REJECTED)).isFalse();
    }

    @Test
    @DisplayName("승낙 알림은 강사(sender)가 학생(target)에게 보낸다. 방향이 뒤바뀌면 안 된다")
    void 강사가_학생에게_보낸다() {
        NotificationCommand command = new NotificationCommand(100L, 7L, 42L, "자료구조 특강", "수강 신청이 승낙되었습니다");

        acceptEnrollmentStrategy.handle(command);

        // 인자 값을 정확히 검증한다. any()로 두면 sender/target이 뒤바뀌어도 통과한다.
        verify(notificationFacade).sendNotification(
                42L,            // sender: tutorId
                List.of(7L),    // target: studentId
                "자료구조 특강",
                "수강 신청이 승낙되었습니다"
        );
    }
}
