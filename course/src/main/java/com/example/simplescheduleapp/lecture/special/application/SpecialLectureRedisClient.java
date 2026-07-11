package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 1차 방어선: Redis Atomic Counter 기반 정원 사전 필터링.
 * <p>
 * 10,000명이 정원 100명 특강에 동시 신청해도, 9,900명은 Redis 단계에서 즉시 거절된다.
 * DB까지 도달하는 요청을 정원 수준으로 줄여 DB 부하를 99% 감소시킨다.
 * <p>
 * 신청/보상은 Lua 기반 조건부 연산을 사용한다:
 * <ul>
 *   <li>키 부재(-2)를 정원 초과와 구분 — 만료·미초기화 키를 음수로 생성하지 않음</li>
 *   <li>정원 소진(-1) 시 감소 자체가 일어나지 않아 INCR 복구 왕복이 불필요</li>
 *   <li>보상은 키가 존재할 때만 증가 — 만료 키를 TTL 없이 부활시키지 않음</li>
 * </ul>
 * 정원 캐시 TTL은 특강 종료 시각 + 1일로 수명을 정렬하고, 계산 불가 시 30일로 보정한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SpecialLectureRedisClient {

    private static final String AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX = "special_lecture:%d:available";

    /** 키 부재를 나타내는 Lua 스크립트 반환값 */
    private static final long KEY_NOT_FOUND = -2L;

    /** 정원 소진을 나타내는 Lua 스크립트 반환값 */
    private static final long CAPACITY_EXHAUSTED = -1L;

    /** endTime 기반 TTL을 계산할 수 없을 때(과거 시각 등)의 보정값 */
    private static final Duration FALLBACK_CAPACITY_TTL = Duration.ofDays(30);

    private final AtomicCounter atomicCounter;

    // 특강 생성 시 수강 정원 설정 — TTL은 특강 종료 + 1일로 수명 정렬
    public void initializeSpecialLecture(Long specialLectureId, int capacity, LocalDateTime endTime) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        Duration ttl = Duration.between(LocalDateTime.now(), endTime.plusDays(1));
        if (ttl.isZero() || ttl.isNegative()) {
            ttl = FALLBACK_CAPACITY_TTL;
        }
        atomicCounter.set(key, capacity, ttl);
    }

    // 특강 수강 신청 시도 --> 정원 체크 및 카운트 감소 (원자적 check-and-decrement)
    public void enrollSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        Long result = atomicCounter.decrementIfPositive(key);

        if (result == null || result == KEY_NOT_FOUND) {
            throw new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS);
        }
        if (result == CAPACITY_EXHAUSTED) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    // 레디스는 성공했는데 특강 DB 저장 실패 시 레디스 값 원상복구.
    // 키 부재 시 예외를 던지면 원인이 된 DB 예외를 가리므로 경고 로그만 남긴다.
    public void compensateSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        Long result = atomicCounter.incrementIfExists(key);

        if (result == null || result == KEY_NOT_FOUND) {
            log.warn("보상 대상 Redis 정원 키가 없어 복구를 건너뜀 (만료 추정). specialLectureId={}", specialLectureId);
        }
    }

    private String buildAvailableSpecialLectureCapacityKey(Long specialLectureId) {
        return String.format(AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX, specialLectureId);
    }
}
