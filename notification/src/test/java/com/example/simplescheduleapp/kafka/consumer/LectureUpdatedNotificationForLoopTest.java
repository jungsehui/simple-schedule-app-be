package com.example.simplescheduleapp.kafka.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayName("단순 반복문 알림 처리 성능 테스트")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LectureUpdatedNotificationForLoopTest extends AbstractNotificationPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(LectureUpdatedNotificationForLoopTest.class);

    @Test
    void 반복문으로_알림_전송_테스트() throws InterruptedException {
        // when
        stopWatch.start();
        produceMessage(); // 카프카 메시지 발행
        boolean await = latch.await(60, TimeUnit.SECONDS); // 최대 60초 대기
        stopWatch.stop();

        // then
        assertThat(await).isTrue(); // 시간 내에 모든 작업이 완료되었는지 확인
        log.info("단순 반복문 총 소요 시간: %d ms%n".formatted(stopWatch.getTotalTimeMillis()));
    }
}
