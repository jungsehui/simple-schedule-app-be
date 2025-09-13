package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.kafka.producer.SpecialLectureEnrollmentSuccessTopicProducer;
import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.infra.SpecialLectureRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSpecialLectureEnrollmentService {

    private final SpecialLectureRedisClient specialLectureRedisClient;
    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;
    private final SpecialLectureEnrollmentSuccessTopicProducer specialLectureEnrollmentSuccessTopicProducer;

    // 바로 처리하기
    public void enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        specialLectureRedisClient.enrollSpecialLectureEnrollment(command.specialLectureId());
        specialLectureEnrollmentService.enrollSpecialLectureEnrollment(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }

    // 카프카로 쏘기
    public void enrollSpecialLectureEnrollmentKafka(SpecialLectureEnrollmentCreateCommand command) {
        specialLectureRedisClient.enrollSpecialLectureEnrollment(command.specialLectureId());
        specialLectureEnrollmentSuccessTopicProducer.produce(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }
}
