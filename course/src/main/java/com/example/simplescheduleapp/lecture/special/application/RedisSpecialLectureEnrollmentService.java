package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSpecialLectureEnrollmentService {

    private final SpecialLectureRedisClient specialLectureRedisClient;
    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;

    @Transactional
    public void enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        specialLectureRedisClient.enrollSpecialLectureEnrollment(command.specialLectureId());

        try {
            SpecialLectureEnrollment specialLectureEnrollment =
                    specialLectureEnrollmentService.enrollSpecialLectureEnrollment(command.specialLectureId(), command.studentId());
            log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.",
                    specialLectureEnrollment.getStudent().getId(), specialLectureEnrollment.getSpecialLecture().getId());
        } catch (Exception e) {
            // DB 저장이 실패했다면 무조건 Redis를 복구해야 함
            // 중복 신청(DataIntegrityViolationException)이든, 다른 DB 에러든
            // DB에는 저장이 안 됐으므로 Redis 숫자를 다시 돌려놔야 정합성이 맞음
            log.error("DB 저장 실패 .. Redis 보상 트랜잭션 수행. cause: {}", e.getClass().getSimpleName());
            specialLectureRedisClient.compensateSpecialLectureEnrollment(command.specialLectureId());

            throw switch (e) {
                case DataIntegrityViolationException dataIntegrityViolationException ->
                        new ApplicationException(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED);
                case ApplicationException applicationException ->
                        applicationException;
                default ->
                        new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
            };

//            if (e instanceof DataIntegrityViolationException) {
//                throw new ApplicationException(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED);
//            } else if (e instanceof ApplicationException) {
//                throw (ApplicationException) e;
//            } else {
//                throw new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
//            }
        }
    }
}
