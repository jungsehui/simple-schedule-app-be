package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.special.application.RedisSpecialLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureEnrollmentController {

    private final RedisSpecialLectureEnrollmentService redisSpecialLectureEnrollmentService;

    // Phase 3a 듀얼리드: 토큰 식별자 우선, studentId 파라미터는 레거시 폴백 (3b에서 제거 예정)
    @RequireRole(Role.STUDENT)
    @PostMapping("/special-lectures/{specialLectureId}/enrollments")
    public ResponseEntity<Void> enrollSpecialLectureEnrollment(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long studentId,
            @PathVariable Long specialLectureId
    ) {
        Long resolvedStudentId = AuthIdentities.resolve(memberId, studentId);
        log.info("특강 요청이 들어왔습니다. 학생 ID: {}", resolvedStudentId);
        SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(resolvedStudentId, specialLectureId);
        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment(command);
//        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollmentKafka(command);
        return ResponseEntity.ok().build();
    }
}
