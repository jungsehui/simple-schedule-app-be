package com.example.simplescheduleapp.kafka.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("단순 반복문 알림 처리 성능 테스트")
class LectureUpdatedNotificationKafkaForLoopTest extends AbstractNotificationPerformanceTest {

    @Test
    void testSequentialPerformance() throws InterruptedException {
        // when
        stopWatch.start();
        produceMessage(); // 카프카 메시지 발행
        boolean await = latch.await(60, TimeUnit.SECONDS); // 최대 60초 대기
        stopWatch.stop();

        // then
        assertThat(await).isTrue(); // 시간 내에 모든 작업이 완료되었는지 확인
        System.out.printf("단순 반복문 총 소요 시간: %d ms%n", stopWatch.getTotalTimeMillis());
    }
}
