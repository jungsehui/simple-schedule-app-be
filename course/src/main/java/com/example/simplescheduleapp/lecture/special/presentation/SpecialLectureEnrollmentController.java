package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.MemberRole;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.lecture.special.application.RedisSpecialLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureEnrollmentController {

    private final RedisSpecialLectureEnrollmentService redisSpecialLectureEnrollmentService;

    @RequireRole(MemberRole.STUDENT)
    @PostMapping("/special-lectures/{specialLectureId}/enrollments")
    public ResponseEntity<Void> enrollSpecialLectureEnrollment(
            @Auth Long memberId,
            @PathVariable Long specialLectureId
    ) {
        log.info("특강 요청이 들어왔습니다. 학생 ID: {}", memberId);
        SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(memberId, specialLectureId);
        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment(command);
//        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollmentKafka(command);
        return ResponseEntity.ok().build();
    }
}
