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
@DisplayName("Parallel Stream 알림 처리 성능 테스트")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LectureUpdatedNotificationParallelStreamTest extends AbstractNotificationPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(LectureUpdatedNotificationParallelStreamTest.class);

    @Test
    void ForkJoinPool_활용_알림_전송_테스트() throws InterruptedException {
        // when
        stopWatch.start();
        produceMessage();
        boolean await = latch.await(10, TimeUnit.SECONDS); // 대기 시간 단축
        stopWatch.stop();

        // then
        assertThat(await).isTrue();
        log.info("Parallel Stream 총 소요 시간: %d ms%n".formatted(stopWatch.getTotalTimeMillis()));
    }
}
