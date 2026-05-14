package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 1차 방어선: Redis Atomic Counter 기반 정원 사전 필터링.
 * <p>
 * 10,000명이 정원 100명 특강에 동시 신청해도, 9,900명은 Redis DECR 단계에서 즉시 거절된다.
 * DB까지 도달하는 요청을 정원 수준으로 줄여 DB 부하를 99% 감소시킨다.
 * <p>
 * Redis 키 메모리 누수를 막기 위해 정원 캐시에 TTL을 부여한다.
 * 기본값은 30일이며, 특강 종료 후 자동으로 정리되도록 설계되어 있다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SpecialLectureRedisClient {

    private static final String AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX = "special_lecture:%d:available";

    /**
     * 정원 캐시 TTL.
     * <p>
     * 특강 종료 후에도 일정 기간 캐시를 유지하여, 종료 직후 보상 트랜잭션이나 조회가
     * 안전하게 동작할 수 있도록 한다. 30일 후에는 자동으로 정리된다.
     */
    private static final Duration AVAILABLE_CAPACITY_TTL = Duration.ofDays(30);

    private final AtomicCounter atomicCounter;

    // 특강 생성 시 수강 정원 설정 (TTL 포함)
    public void initializeSpecialLecture(Long specialLectureId, int capacity) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        atomicCounter.set(key, capacity, AVAILABLE_CAPACITY_TTL);
    }

    // 특강 수강 신청 시도 --> 정원 체크 및 카운트 감소
    public void enrollSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);

        // 값을 1 감소시키고, 감소된 후의 값을 받아서
        Long remainingCapacity = atomicCounter.decrement(key);

        validateRedisResult(remainingCapacity);

        // 남은 자리가 0보다 작다면? 즉, -1이 되었다면 정원 초과
        if (remainingCapacity < 0) {
            // 다시 1을 더해서 0으로 맞춰줌
            // 사실 이 부분은 굳이 안 해도 되는 거 같긴 한데, 깔끔한 데이터를 위해 복구
            atomicCounter.increment(key);

            // 수강 신청 불가능 예외 처리
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    // 레디스는 성공했는데 특강 DB 저장 실패 시 레디스 값 원상복구
    public void compensateSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        atomicCounter.increment(key);
    }

    // 만약 값이 안 돌아 온다면 예외 처리
    private void validateRedisResult(Long val) {
        if (val == null) {
            throw new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
        }
    }

    private String buildAvailableSpecialLectureCapacityKey(Long specialLectureId) {
        return String.format(AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX, specialLectureId);
    }
}
