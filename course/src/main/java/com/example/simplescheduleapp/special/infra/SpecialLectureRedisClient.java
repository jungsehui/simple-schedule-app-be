package com.example.simplescheduleapp.special.infra;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SpecialLectureRedisClient {

    private static final String ENROLLED_COUNT_KEY_PREFIX = "special_lecture:%d:enrolled_count";
    private static final String CAPACITY_KEY_PREFIX = "special_lecture:%d:capacity";
    private static final String INITIAL_ENROLLED_COUNT = "0";

    private final StringRedisTemplate stringRedisTemplate;

    // 특강 생성 시 레디스 템플릿에 정원과 초기 신청 인원 수를 설정
    public void initializeSpecialLecture(Long specialLectureId, int capacity) {
        String countKey = buildEnrolledCountKey(specialLectureId);
        String capacityKey = buildCapacityKey(specialLectureId);

        // 초기 값
        stringRedisTemplate.opsForValue().set(countKey, INITIAL_ENROLLED_COUNT);
        stringRedisTemplate.opsForValue().set(capacityKey, String.valueOf(capacity));
    }

    // 특강 수강 신청 시도 (정원 체크 및 카운트 증가)
    public void enrollSpecialLectureEnrollment(Long specialLectureId) {
        String countKey = buildEnrolledCountKey(specialLectureId);
        String capacityKey = buildCapacityKey(specialLectureId);

        // 신청 인원을 1 증가
        Long currentCount = stringRedisTemplate.opsForValue().increment(countKey);
        validateCurrentCount(currentCount);

        // 정원 정보를 가져 옴
        String capacityStr = stringRedisTemplate.opsForValue().get(capacityKey);
        validateCapacityStrNull(capacityStr, countKey);

        // 정원을 초과했는지 확인
        long capacity = Long.parseLong(capacityStr);
        validateCurrentCountOverCapacity(currentCount, capacity, countKey);
    }

    private void validateCurrentCount(Long currentCount) {
        // 안정성을 위해 유지
        if (currentCount == null) {
            throw new RuntimeException("Redis 처리 중 오류가 발생했습니다.");
        }
    }

    private void validateCapacityStrNull(String capacityStr, String countKey) {
        if (capacityStr == null) {
            // 정원 정보가 없는 경우, 증가시켰던 카운트를 다시 원복 (보상 트랜잭션)
            stringRedisTemplate.opsForValue().decrement(countKey);
            throw new ApplicationException(LectureExceptionCode.CAPACITY_INFO_NOT_FOUND);
        }
    }

    private void validateCurrentCountOverCapacity(long currentCount, long capacity, String countKey) {
        if (currentCount > capacity) {
            // 정원이 초과되면 즉시 카운트를 원복
            stringRedisTemplate.opsForValue().decrement(countKey);
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    private String buildEnrolledCountKey(Long specialLectureId) {
        return String.format(ENROLLED_COUNT_KEY_PREFIX, specialLectureId);
    }

    private String buildCapacityKey(Long specialLectureId) {
        return String.format(CAPACITY_KEY_PREFIX, specialLectureId);
    }
}
