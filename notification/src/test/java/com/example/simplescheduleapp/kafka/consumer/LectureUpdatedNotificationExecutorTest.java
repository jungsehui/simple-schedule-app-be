package com.example.simplescheduleapp.kafka.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutorService 알림 처리 성능 테스트")
public class LectureUpdatedNotificationExecutorTest extends AbstractNotificationPerformanceTest {

    @Test
    void testExecutorServicePerformance() throws InterruptedException {
        // when
        stopWatch.start();
        produceMessage();
        boolean await = latch.await(10, TimeUnit.SECONDS); // 대기 시간 단축
        stopWatch.stop();

        // then
        assertThat(await).isTrue();
        System.out.printf("ExecutorService 총 소요 시간: %d ms%n", stopWatch.getTotalTimeMillis());
    }
}