package com.example.simplescheduleapp.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 순수 도메인 {@code FailedNotification} 단위 테스트 (ADR-0004).
 * <p>인프라 없이 순수 자바만으로 검증된다 — 프레임워크/DB 부트스트랩 불필요.
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FailedNotificationTest {

    @Test
    @DisplayName("신규 생성 시 retryCount=1 이고 uuid가 부여되며 id는 아직 없다")
    void 신규_생성_초기상태() {
        FailedNotification failed = new FailedNotification(1L, 2L, "제목", "본문", NotificationType.FCM, "전송 실패");

        assertThat(failed.getId()).isNull();
        assertThat(failed.getUuid()).isNotBlank();
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getSenderId()).isEqualTo(1L);
        assertThat(failed.getTargetId()).isEqualTo(2L);
        assertThat(failed.getType()).isEqualTo(NotificationType.FCM);
    }

    @Test
    @DisplayName("incrementRetryCount 는 재시도 횟수를 1 증가시킨다")
    void 재시도_증가() {
        FailedNotification failed = new FailedNotification(1L, 2L, "제목", "본문", NotificationType.SSE, "이유");

        failed.incrementRetryCount();

        assertThat(failed.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("reconstitute 는 DB 복원 상태를 그대로 담는다")
    void 재구성_복원() {
        FailedNotification failed = FailedNotification.reconstitute(
                10L, "uuid-x", 1L, 2L, "제목", "본문", NotificationType.FCM, "이유", 3);

        assertThat(failed.getId()).isEqualTo(10L);
        assertThat(failed.getUuid()).isEqualTo("uuid-x");
        assertThat(failed.getRetryCount()).isEqualTo(3);
    }
}
