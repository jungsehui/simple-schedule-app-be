package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.special.application.RedisSpecialLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureEnrollmentController {

    private final RedisSpecialLectureEnrollmentService redisSpecialLectureEnrollmentService;

    @RequireRole(Role.STUDENT)
    @PostMapping("/special-lectures/{specialLectureId}/enrollments")
    public ResponseEntity<Void> enrollSpecialLectureEnrollment(
            @Auth Long memberId,
            @RequestParam(required = false) Long studentId, // 레거시 — 수용하되 무시 (ADR-0005)
            @PathVariable Long specialLectureId
    ) {
        log.info("특강 요청이 들어왔습니다. 학생 ID: {}", memberId);
        SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(memberId, specialLectureId);
        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment(command);
//        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollmentKafka(command);
        return ResponseEntity.ok().build();
    }

    /**
     * 특강 수강신청 취소.
     *
     * <p>종전에는 취소 경로가 없어 <b>오신청이 영구</b>였고 좌석도 반환되지 않았다.
     * 일반 강의에는 {@code DELETE /lectures/{lectureId}/enrollments}가 있는데 특강만 없던
     * 비대칭이다.
     *
     * <p>취소 대상은 <b>토큰의 주체</b>다. 경로에도 파라미터에도 남의 식별자를 넣을 자리가
     * 없다(ADR-0005). {@code studentId} 쿼리 파라미터는 하위호환으로 받되 쓰지 않는다.
     */
    @RequireRole(Role.STUDENT)
    @DeleteMapping("/special-lectures/{specialLectureId}/enrollments")
    public ResponseEntity<Void> cancelSpecialLectureEnrollment(
            @Auth Long memberId,
            @RequestParam(required = false) Long studentId, // 레거시 — 수용하되 무시 (ADR-0005)
            @PathVariable Long specialLectureId
    ) {
        SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(memberId, specialLectureId);
        redisSpecialLectureEnrollmentService.cancelSpecialLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }
}
