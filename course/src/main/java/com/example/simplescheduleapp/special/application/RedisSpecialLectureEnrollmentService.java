package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.infra.SpecialLectureRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSpecialLectureEnrollmentService {

    private final SpecialLectureRedisClient specialLectureRedisClient;
    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;

    public void enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        specialLectureRedisClient.enrollSpecialLectureEnrollment(command.specialLectureId());
        specialLectureEnrollmentService.enrollSpecialLectureEnrollment(command.specialLectureId(), command.studentId());
        log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.", command.studentId(), command.specialLectureId());
    }
}
