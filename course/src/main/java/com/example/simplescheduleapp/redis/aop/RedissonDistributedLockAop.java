package com.example.simplescheduleapp.redis.aop;

import com.example.simplescheduleapp.common.aop.AopForTransaction;
import com.example.simplescheduleapp.redis.lock.RedissonDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@RequiredArgsConstructor
@Component
@Aspect
public class RedissonDistributedLockAop {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(com.example.simplescheduleapp.redis.lock.RedissonDistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonDistributedLock distributedLock = method.getAnnotation(RedissonDistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                joinPoint.getArgs(),
                signature.getParameterNames(),
                distributedLock.key()
        );
        RLock lock = redissonClient.getLock(key);

        try {
            boolean available = lock.tryLock(
                    distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit()
            );
            if (!available) {
                log.warn("Redisson Lock 획득 실패. key: {}", key);
                throw new IllegalStateException("Lock을 획득할 수 없습니다 ..");
            }

            return aopForTransaction.proceed(joinPoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("락 대기 중 인터럽트 발생. e: {}".formatted(e));
        } finally {
            try {
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock. serviceName: {}, key: {}", method.getName(), key);
            }
        }
    }
}
