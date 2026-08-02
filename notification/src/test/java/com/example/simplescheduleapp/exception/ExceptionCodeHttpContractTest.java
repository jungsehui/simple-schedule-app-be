package com.example.simplescheduleapp.exception;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.CommonExceptionHandler;
import com.example.simplescheduleapp.common.exception.ExceptionCode;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;
import com.example.simplescheduleapp.notification.exception.FailedNotificationExceptionCode;
import com.example.simplescheduleapp.notification.exception.NotificationTypeExceptionCode;
import com.example.simplescheduleapp.sse.exception.SseExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * 예외 코드 → HTTP 상태 특성화 테스트 (notification 소유 5개).
 *
 * <p>상태 코드는 클라이언트 노출 계약(API-CONTRACT.md)이다. 예외 코드 내부 표현이 바뀌어도
 * 핸들러를 통과한 최종 상태 코드는 변하면 안 된다.
 */
@DisplayName("예외 코드 HTTP 상태 계약(notification) 은(는)")
class ExceptionCodeHttpContractTest {

    private final CommonExceptionHandler handler = new CommonExceptionHandler();

    @DisplayName("리팩터링 전과 동일한 상태 코드를 유지한다")
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("contract")
    void statusCodeContractIsPreserved(ExceptionCode code, HttpStatus expected) {
        assertThat(handler.handleApplicationException(new ApplicationException(code)).getStatusCode())
                .isEqualTo(expected);
    }

    static Stream<Arguments> contract() {
        return Stream.of(
                arguments(SseExceptionCode.SSE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(SseExceptionCode.SSE_SEND_FAILED, HttpStatus.CONFLICT),
                arguments(NotificationTypeExceptionCode.NOTIFICATION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(FailedNotificationExceptionCode.FAILED_NOTIFICATION_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
