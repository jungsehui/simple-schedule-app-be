package com.example.simplescheduleapp.kafka.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Parallel Stream 알림 처리 성능 테스트")
public class LectureUpdatedNotificationParallelStreamTest extends AbstractNotificationPerformanceTest {

    @Test
    void testParallelStreamPerformance() throws InterruptedException {
        // when
        stopWatch.start();
        produceMessage();
        boolean await = latch.await(10, TimeUnit.SECONDS); // 대기 시간 단축
        stopWatch.stop();

        // then
        assertThat(await).isTrue();
        System.out.printf("Parallel Stream 총 소요 시간: %d ms%n", stopWatch.getTotalTimeMillis());
    }
}