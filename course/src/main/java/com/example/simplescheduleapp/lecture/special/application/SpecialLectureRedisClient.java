package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SpecialLectureRedisClient {

    private static final String AVAILABLE_SPECIAL_LECTURE_CAPACITY_KEY_PREFIX = "special_lecture:%d:available";

    private final StringRedisTemplate stringRedisTemplate;

    // 특강 생성 시 수강 정원 설정
    public void initializeSpecialLecture(Long specialLectureId, int capacity) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(capacity));
    }

    // 특강 수강 신청 시도 --> 정원 체크 및 카운트 감소
    public void enrollSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);

        // 값을 1 감소시키고, 감소된 후의 값을 받아서
        Long remainingCapacity = stringRedisTemplate.opsForValue().decrement(key);

        validateRedisResult(remainingCapacity);

        // 남은 자리가 0보다 작다면? 즉, -1이 되었다면 정원 초과
        if (remainingCapacity < 0) {
            // 다시 1을 더해서 0으로 맞춰줌
            // 사실 이 부분은 굳이 안 해도 되는 거 같긴 한데, 깔끔한 데이터를 위해 복구
            stringRedisTemplate.opsForValue().increment(key);

            // 수강 신청 불가능 예외 처리
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }

    // 레디스는 성공했는데 특강 DB 저장 실패 시 레디스 값 원상복구
    public void compensateSpecialLectureEnrollment(Long specialLectureId) {
        String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
        stringRedisTemplate.opsForValue().increment(key);
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
