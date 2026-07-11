package com.example.playground.requiresnewdeadlock;

import com.example.playground.PlaygroundApplication;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ActiveProfiles("requires-new-deadlock-not-occur")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = PlaygroundApplication.class)
public class RequiresNewDeadLockNotOccurTest {

    @Autowired
    private DeadLockTestEventProducer deadLockTestEventProducer;

    @Test
    void 데드락이_발생하지_않는다() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when
        // 쓰레드: 이벤트 2개 발행
        for (int i = 0; i < 2; i++) {
            final long id = i + 1;
            executorService.submit(() -> {
                try {
                    deadLockTestEventProducer.publishEvent(new TestEvent(id));
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 두 스레드의 작업이 끝날 때까지 대기

        // then
        // 예외가 발생하지 않음
    }
}
