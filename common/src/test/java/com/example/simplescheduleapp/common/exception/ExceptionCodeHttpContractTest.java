package com.example.simplescheduleapp.common.exception;

import com.example.simplescheduleapp.common.auth.TokenExceptionCode;
import com.example.simplescheduleapp.common.event.exception.DomainEventExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * 예외 코드 → HTTP 상태 특성화 테스트 (common 소유 13개).
 *
 * <p>상태 코드는 클라이언트 노출 계약(API-CONTRACT.md)이다. 예외 코드 내부 표현이 바뀌어도
 * (HttpStatus → ErrorKind 등) 핸들러를 통과한 최종 상태 코드는 변하면 안 된다 — 이 테스트가
 * 그 계약을 리팩터링 전 스냅샷으로 고정한다.
 */
@DisplayName("예외 코드 HTTP 상태 계약(common) 은(는)")
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
                arguments(TokenExceptionCode.REQUIRED_TOKEN, HttpStatus.UNAUTHORIZED),
                arguments(TokenExceptionCode.EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED),
                arguments(TokenExceptionCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED),
                arguments(TokenExceptionCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
                arguments(TokenExceptionCode.REQUIRED_BEARER_TOKEN, HttpStatus.UNAUTHORIZED),
                arguments(TokenExceptionCode.UNKNOWN_TOKEN, HttpStatus.INTERNAL_SERVER_ERROR),
                arguments(TokenExceptionCode.FORBIDDEN, HttpStatus.FORBIDDEN),
                arguments(TokenExceptionCode.REQUIRED_ROLE_CLAIM, HttpStatus.FORBIDDEN),
                arguments(InternalServerExceptionCode.UNKNOWN_EXCEPTION, HttpStatus.INTERNAL_SERVER_ERROR),
                arguments(InternalServerExceptionCode.EXTERNAL_API_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
                arguments(InternalServerExceptionCode.INVALID_INPUT_VALUE, HttpStatus.BAD_REQUEST),
                arguments(InternalServerExceptionCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(DomainEventExceptionCode.DOMAIN_EVENT_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(DomainEventExceptionCode.DOMAIN_EVENT_NOT_SUPPORTED, HttpStatus.NOT_FOUND));
    }
}
