package com.example.simplescheduleapp.lecture.special.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 동시성 제어 4단계 방어 구조의 Feature Toggle.
 * <p>
 * K6 부하 테스트 시 시나리오별 비교를 위해 각 방어선을 on/off 할 수 있도록 한다.
 *
 * <ul>
 *   <li><b>시나리오 A (baseline)</b>: distributedLock=false, optimisticLockHandling=false
 *       → 1차 Redis Atomic + 4차 UK 제약만. 현재 운영 코드 동작과 동일.</li>
 *   <li><b>시나리오 B (분산 락)</b>: distributedLock=true, optimisticLockHandling=false
 *       → 1차 + 2차 Redisson FairLock + 4차. 락이 직렬화하므로 OptimisticLock은 트리거되지 않음.</li>
 *   <li><b>시나리오 C (전체 4단계)</b>: distributedLock=true, optimisticLockHandling=true
 *       → 1차 + 2차 + 3차 낙관적 락 명시적 처리 + 4차.
 *       락 leaseTime 만료/네트워크 파티션 등 비정상 상황에서 OptimisticLockException을 보상 처리.</li>
 * </ul>
 *
 * <h3>왜 낙관적 락은 별도 toggle인가</h3>
 * @Version 필드는 엔티티에 항상 존재하므로 런타임 toggle이 불가능하다.
 * 대신 OptimisticLockException 발생 시 <b>명시적으로 보상 트랜잭션을 수행할지 여부</b>를
 * 본 toggle로 제어한다.
 *
 * @param distributedLock         2차 방어선(분산 락) 활성화 여부
 * @param optimisticLockHandling  3차 방어선(낙관적 락) 예외에 대한 명시적 보상 처리 여부
 */
@ConfigurationProperties(prefix = "defense")
public record DefenseProperties(
        @DefaultValue("true") boolean distributedLock,
        @DefaultValue("true") boolean optimisticLockHandling
) {
}
