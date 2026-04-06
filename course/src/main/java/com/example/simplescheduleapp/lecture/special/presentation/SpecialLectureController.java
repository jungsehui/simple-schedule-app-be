package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.MemberRole;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.lecture.special.application.SpecialLectureService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.presentation.request.SpecialLectureCreateRequest;
import com.example.simplescheduleapp.lecture.special.presentation.response.SpecialLectureCreateResponse;
import com.example.simplescheduleapp.redis.lock.RedissonDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureController {

    private final SpecialLectureService specialLectureService;

    @RequireRole(MemberRole.TUTOR)
    @PostMapping("/special-lectures")
    public ResponseEntity<SpecialLectureCreateResponse> createSpecialLecture(
            @Auth Long memberId,
            @RequestBody SpecialLectureCreateRequest request
    ) {
        SpecialLectureCreateCommand command = request.toCommand(memberId);
        SpecialLecture savedSpecialLecture = specialLectureService.createSpecialLecture(command);
        URI location = URI.create("/special-lectures/" + savedSpecialLecture.getId());
        return ResponseEntity
                .created(location)
                .body(SpecialLectureCreateResponse.from(savedSpecialLecture));
    }
}
