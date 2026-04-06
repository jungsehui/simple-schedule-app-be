package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SpecialLectureRedisClient {

    private static final String AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX = "special_lecture:%d:available";

    /**
     * 원자적 check-and-decrement Lua 스크립트.
     * 키가 존재하지 않으면 -2 반환, 남은 정원이 0 이하면 -1 반환, 성공 시 감소된 값 반환.
     */
    private static final DefaultRedisScript<Long> ENROLL_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('get', KEYS[1])
            if current == false then return -2 end
            if tonumber(current) <= 0 then return -1 end
            return redis.call('decr', KEYS[1])
            """,
            Long.class
    );

    /**
     * 원자적 increment Lua 스크립트 (취소/보상용).
     * 키가 존재하지 않으면 -2 반환, 성공 시 증가된 값 반환.
     */
    private static final DefaultRedisScript<Long> CANCEL_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('get', KEYS[1])
            if current == false then return -2 end
            return redis.call('incr', KEYS[1])
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public void initializeSpecialLecture(Long specialLectureId, int capacity, LocalDateTime endTime) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(capacity));

        long ttlSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime.plusDays(1));
        if (ttlSeconds > 0) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
    }

    public void enrollSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        Long result = stringRedisTemplate.execute(ENROLL_SCRIPT, List.of(key));

        if (result == null || result == -2) {
            throw new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS);
        }
        if (result == -1) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    public void compensateSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        Long result = stringRedisTemplate.execute(CANCEL_SCRIPT, List.of(key));

        if (result == null || result == -2) {
            throw new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS);
        }
    }

    private String buildAvailableSpecialLectureCapacityKey(Long specialLectureId) {
        return String.format(AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX, specialLectureId);
    }
}
