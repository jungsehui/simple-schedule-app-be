package com.example.simplescheduleapp.special.presentation;

import com.example.simplescheduleapp.special.application.RedisSpecialLectureEnrollmentService;
import com.example.simplescheduleapp.special.application.SpecialLectureEnrollmentService;
import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.presentation.response.SpecialLectureEnrollmentCreateResponse;
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

    @PostMapping("/special-lectures/{specialLectureId}/enrollments")
    public ResponseEntity<SpecialLectureEnrollmentCreateResponse> enrollSpecialLectureEnrollment(
            @RequestParam Long studentId,
            @PathVariable Long specialLectureId
    ) {
        log.info("특강 요청이 들어왔습니다. 학생 ID: {}", studentId);
        SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(studentId, specialLectureId);
        redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment(command);
        return ResponseEntity.ok().build();
    }
}
