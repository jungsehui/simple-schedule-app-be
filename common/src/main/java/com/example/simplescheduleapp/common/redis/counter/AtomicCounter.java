package com.example.simplescheduleapp.common.redis.counter;

import java.time.Duration;

/**
 * 분산 환경에서 원자적(atomic) 카운터 연산을 제공하는 추상화.
 * <p>
 * 1차 방어선(캐시 계층 사전 필터링)의 핵심 컴포넌트.
 * Redis의 INCR/DECR 명령은 단일 명령으로 원자적 실행이 보장되므로,
 * 다중 스레드/다중 인스턴스 환경에서 race condition 없이 정원 차감/복구를 수행할 수 있다.
 */
public interface AtomicCounter {

    /**
     * 키에 값을 설정한다 (TTL 없음).
     * <p>
     * 주의: TTL이 없으면 Redis 메모리 누수 가능성이 있다.
     * 가능한 경우 {@link #set(String, long, Duration)}을 사용해 TTL을 함께 설정하라.
     */
    void set(String key, long value);

    /**
     * 키에 값을 설정하고 TTL을 함께 적용한다.
     * <p>
     * 특강 정원처럼 시간이 지나면 더 이상 유효하지 않은 데이터에 사용한다.
     * 예: 특강 종료 시간 + 24시간으로 TTL 설정 → 특강 종료 후 자동 정리.
     *
     * @param key   Redis 키
     * @param value 초기값
     * @param ttl   유효 기간 (null/zero 불가)
     */
    void set(String key, long value, Duration ttl);

    Long decrement(String key);

    Long increment(String key);

    /**
     * 키가 존재하고 값이 양수일 때만 원자적으로 1 감소시킨다 (check-and-decrement).
     * <p>
     * 단순 DECR과 달리 부재 키를 음수로 생성하지 않으므로,
     * 키 만료/미초기화 상태를 정원 초과로 오분류하지 않는다.
     *
     * @return 감소된 값. 키가 없으면 -2, 남은 수량이 0 이하면 -1 (감소 없음)
     */
    Long decrementIfPositive(String key);

    /**
     * 키가 존재할 때만 원자적으로 1 증가시킨다.
     * <p>
     * 단순 INCR과 달리 만료·부재 키를 TTL 없는 키로 부활시키지 않는다 (보상 경로용).
     *
     * @return 증가된 값. 키가 없으면 -2
     */
    Long incrementIfExists(String key);
}
