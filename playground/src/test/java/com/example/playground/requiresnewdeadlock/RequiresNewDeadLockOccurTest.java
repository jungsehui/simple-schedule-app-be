package com.example.playground.requiresnewdeadlock;

import com.example.playground.PlaygroundApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Disabled // 데드락 확인 시에만 풀기
@ActiveProfiles("requires-new-deadlock-occur")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = PlaygroundApplication.class)
public class RequiresNewDeadLockOccurTest {

    @Autowired
    private DeadLockTestEventProducer deadLockTestEventProducer;

    @Test
    void 데드락이_발생한다() throws InterruptedException {
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
        // 데드락으로 인해 커넥션 타임아웃 예외가 발생하는 것을 검증
        // 실제로는 두 스레드 중 하나에서 예외가 발생하므로, 검증 로직은 생략하거나
        // Future 등을 통해 예외를 받아와서 검증할 수 있습니다.
        // 여기서는 데드락 현상을 재현하는 것에 초점을 맞춥니다.
        // 실제 테스트 실행 시 로그에 타임아웃 에러가 출력됩니다.
    }
}
