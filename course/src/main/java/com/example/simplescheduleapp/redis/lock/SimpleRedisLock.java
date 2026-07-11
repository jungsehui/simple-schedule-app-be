package com.example.simplescheduleapp.redis.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SimpleRedisLock {

    // ------------------------- LUA SCRIPTS -------------------------

    // 락 획득 스크립트 (Reentrancy 지원: Hash 구조 사용)
    // KEYS[1]: lockKey
    // ARGV[1]: leaseTime, ARGV[2]: threadId
    private static final String LOCK_SCRIPT =
            "if (redis.call('exists', KEYS[1]) == 0) then " +
                    "   redis.call('hset', KEYS[1], ARGV[2], 1); " +
                    "   redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "   return nil; " +
                    "end; " +
                    "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                    "   redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                    "   redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "   return nil; " +
                    "end; " +
                    "return redis.call('pttl', KEYS[1]);";

    // 락 해제 스크립트
    // KEYS[1]: lockKey, KEYS[2]: channelName
    // ARGV[1]: leaseTime, ARGV[2]: threadId
    // 반환값: 1(해제 완료), 0(아직 락 보유중-카운트 감소), null(락 소유자 아님)
    private static final String UNLOCK_SCRIPT =
            "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
                    "   return nil; " +
                    "end; " +
                    "local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1); " +
                    "if (counter > 0) then " +
                    "   redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "   return 0; " +
                    "else " +
                    "   redis.call('del', KEYS[1]); " +
                    "   redis.call('publish', KEYS[2], 'UNLOCK'); " +
                    "   return 1; " +
                    "end;";

    // ---------------------------------------------------------------

    private final String lockKey;
    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer; // 리스너 등록을 위해 필요
    private final String channelName;

    public boolean tryLock(long waitTime, long leaseTime) throws InterruptedException {
        long waitTimeMillis = waitTime * 1000;
        long endTime = System.currentTimeMillis() + waitTimeMillis;
        String threadId = String.valueOf(Thread.currentThread().threadId());

        // 1. 락 획득 시도 (Lua Script 실행)
        Long ttl = tryAcquire(leaseTime, threadId);

        // 락 획득 성공
        if (ttl == null) {
            return true;
        }

        // 2. 락 획득 실패 시 Pub/Sub 구독 준비
        // 대기 상태를 제어할 Semaphore 생성 (permit 0으로 시작)
        Semaphore semaphore = new Semaphore(0);

        // 메시지가 오면 세마포어의 permit을 1 증가시키는 리스너 정의
        MessageListener messageListener = (message, pattern) -> {
            // 락 해제 메시지를 받으면 세마포어를 풀어줌
            semaphore.release();
        };

        try {
            // 채널 구독
            redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(channelName));

            while (true) {
                long currentTime = System.currentTimeMillis();
                long remainingTime = endTime - currentTime;

                if (remainingTime <= 0) {
                    return false; // 대기 시간 초과
                }

                // 구독 직후, 혹은 깨어난 직후 락을 다시 시도해봄 (중요)
                ttl = tryAcquire(leaseTime, threadId);
                if (ttl == null) {
                    return true;
                }

                // TTL이 남아있고, 아직 전체 대기시간(waitTime)도 남았다면 대기
                if (ttl > 0 && ttl < remainingTime) {
                    // Pub/Sub 메시지가 올 때까지 혹은 ttl 만큼만 기다림
                    // tryAcquire(time)은 시간 내에 permit을 획득하면 true, 아니면 false 반환
                    // 메시지가 오면(release) 즉시 리턴되어 루프를 다시 돔
                    semaphore.tryAcquire(ttl, TimeUnit.MILLISECONDS);
                } else {
                    // TTL이 remainingTime보다 길다면, 남은 시간만큼만 대기
                    semaphore.tryAcquire(remainingTime, TimeUnit.MILLISECONDS);
                }
            }
        } finally {
            // 3. 반드시 리스너 제거 (메모리 누수 방지)
            redisMessageListenerContainer.removeMessageListener(messageListener);
        }
    }

    private Long tryAcquire(long leaseTime, String threadId) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);

        // 반환값이 null이면 획득 성공, 숫자면 남은 TTL
        return redisTemplate.execute(
                redisScript,
                Collections.singletonList(lockKey),
                String.valueOf(leaseTime),
                threadId
        );
    }

    public void unlock() {
        // 내부적으로 스레드 ID 확인 (자신이 건 락인지 확인용)
        String threadId = String.valueOf(Thread.currentThread().threadId());

        // 기본 30초 등을 leaseTime으로 주거나, 저장해둔 값을 써야 하지만 여기선 예시로 0 처리
        // (Unlock 시에는 leaseTime이 재갱신 용도인데, 완전 삭제될 거라 크게 중요치 않을 수 있음,
        // 단 Reentrancy로 카운트만 줄어들 땐 필요함. 여기선 편의상 하드코딩 혹은 필드값 필요)
        String leaseTimeArg = "30000";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

        // KEYS[1]: lockKey, KEYS[2]: channelName
        redisTemplate.execute(
                redisScript,
                List.of(lockKey, channelName),
                leaseTimeArg,
                threadId
        );
    }
}
