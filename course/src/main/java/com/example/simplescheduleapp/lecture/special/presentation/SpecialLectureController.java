package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.special.application.SpecialLectureService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.presentation.request.SpecialLectureCreateRequest;
import com.example.simplescheduleapp.lecture.special.presentation.response.SpecialLectureCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureController {

    private final SpecialLectureService specialLectureService;

    // Phase 3a 듀얼리드: 토큰 식별자 우선, tutorId 파라미터는 레거시 폴백 (3b에서 제거 예정)
    @RequireRole(Role.TUTOR)
    @PostMapping("/special-lectures")
    public ResponseEntity<SpecialLectureCreateResponse> createSpecialLecture(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long tutorId,
            @Valid @RequestBody SpecialLectureCreateRequest request
    ) {
        SpecialLectureCreateCommand command = request.toCommand(AuthIdentities.resolve(memberId, tutorId));
        SpecialLecture savedSpecialLecture = specialLectureService.createSpecialLecture(command);
        URI location = URI.create("/special-lectures/" + savedSpecialLecture.getId());
        return ResponseEntity
                .created(location)
                .body(SpecialLectureCreateResponse.from(savedSpecialLecture));
    }
}
