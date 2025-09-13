package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.kafka.producer.SpecialLectureEnrollmentSuccessTopicProducer;
import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;

    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;

    private final StringRedisTemplate stringRedisTemplate;
    private final SpecialLectureEnrollmentSuccessTopicProducer specialLectureEnrollmentSuccessTopicProducer;

    // 카프카로 쏘기
    public void enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        String countKey = "special_lecture:" + command.specialLectureId() + ":enrolled_count";
        String capacityKey = "special_lecture:" + command.specialLectureId() + ":capacity";

        Long currentCount = stringRedisTemplate.opsForValue().increment(countKey);
        String capacityStr = stringRedisTemplate.opsForValue().get(capacityKey);
        if (capacityStr == null) {
            stringRedisTemplate.opsForValue().decrement(countKey); // 원복
            throw new RuntimeException("특강 수강 정원 정보가 없습니다.");
        }

        long capacity = Long.parseLong(capacityStr);
        if (currentCount > capacity) {
            // 정원이 초과되면 즉시 카운트 원복
            stringRedisTemplate.opsForValue().decrement(countKey);
            throw new RuntimeException("특강 수강 인원이 마감되었습니다.");
        }

        specialLectureEnrollmentSuccessTopicProducer.produce(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }

    // 비동기로 처리하기
    public void enrollSpecialLectureEnrollmentAsync(SpecialLectureEnrollmentCreateCommand command) {
        String countKey = "special_lecture:" + command.specialLectureId() + ":enrolled_count";
        String capacityKey = "special_lecture:" + command.specialLectureId() + ":capacity";

        Long currentCount = stringRedisTemplate.opsForValue().increment(countKey);
        String capacityStr = stringRedisTemplate.opsForValue().get(capacityKey);
        if (capacityStr == null) {
            stringRedisTemplate.opsForValue().decrement(countKey); // 원복
            throw new RuntimeException("특강 수강 정원 정보가 없습니다.");
        }

        long capacity = Long.parseLong(capacityStr);
        if (currentCount > capacity) {
            // 정원이 초과되면 즉시 카운트 원복
            stringRedisTemplate.opsForValue().decrement(countKey);
            throw new RuntimeException("특강 수강 인원이 마감되었습니다.");
        }

        specialLectureEnrollmentService.enrollSpecialLectureEnrollmentAsync(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }

    // 바로 처리하기
    public void enrollSpecialLectureEnrollmentImmediately(SpecialLectureEnrollmentCreateCommand command) {
        String countKey = "special_lecture:" + command.specialLectureId() + ":enrolled_count";
        String capacityKey = "special_lecture:" + command.specialLectureId() + ":capacity";

        Long currentCount = stringRedisTemplate.opsForValue().increment(countKey);
        String capacityStr = stringRedisTemplate.opsForValue().get(capacityKey);
        if (capacityStr == null) {
            stringRedisTemplate.opsForValue().decrement(countKey); // 원복
            throw new RuntimeException("특강 수강 정원 정보가 없습니다.");
        }

        long capacity = Long.parseLong(capacityStr);
        if (currentCount > capacity) {
            // 정원이 초과되면 즉시 카운트 원복
            stringRedisTemplate.opsForValue().decrement(countKey);
            throw new RuntimeException("특강 수강 인원이 마감되었습니다.");
        }

        specialLectureEnrollmentService.saveSpecialLectureEnrollment(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }
}
