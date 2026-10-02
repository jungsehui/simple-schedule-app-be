package com.example.simplescheduleapp.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영(prod, stage) 로그 인코더 계약: logback-spring.xml의 LogstashEncoder 설정이 JSON 한 줄을 만들고,
 * 요청 추적용 MDC 키(requestId, memberId)를 필드로 싣는다.
 *
 * <p>이 인코더는 prod/stage 프로필에서만 쓰여 다른 테스트로는 검증되지 않는다. logstash-logback-encoder
 * 메이저 업그레이드(9.0: Jackson 2 → 3)처럼 인코더 내부가 바뀔 때 운영 로그가 깨지는 것을 막는다.
 */
@DisplayName("운영 로그 JSON 인코더 은(는)")
class LogstashEncoderJsonTest {

    @Test
    @DisplayName("메시지, 레벨과 설정한 MDC 키를 담은 JSON 한 줄을 만든다")
    void encodesJsonWithConfiguredMdcKeys() {
        LoggerContext context = new LoggerContext();
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        // logback-spring.xml의 prod/stage 설정과 같게
        encoder.addIncludeMdcKeyName("requestId");
        encoder.addIncludeMdcKeyName("memberId");
        encoder.start();

        Logger logger = context.getLogger("contract");
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), logger, Level.INFO, "hello", null, null);
        event.setMDCPropertyMap(Map.of("requestId", "req-1", "memberId", "42", "notIncluded", "x"));

        String json = new String(encoder.encode(event), StandardCharsets.UTF_8);

        assertThat(json).startsWith("{").endsWith("}" + System.lineSeparator());
        assertThat(json)
                .contains("\"message\":\"hello\"")
                .contains("\"level\":\"INFO\"")
                .contains("\"requestId\":\"req-1\"")
                .contains("\"memberId\":\"42\"")
                .doesNotContain("notIncluded");
        encoder.stop();
    }
}
