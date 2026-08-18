package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.special.application.SpecialLectureService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.presentation.request.SpecialLectureCreateRequest;
import com.example.simplescheduleapp.lecture.special.presentation.response.SpecialLectureCreateResponse;
import com.example.simplescheduleapp.lecture.special.presentation.response.SpecialLectureGetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SpecialLectureController {

    private final SpecialLectureService specialLectureService;

    @RequireRole(Role.TUTOR)
    @PostMapping("/special-lectures")
    public ResponseEntity<SpecialLectureCreateResponse> createSpecialLecture(
            @Auth Long memberId,
            @RequestParam(required = false) Long tutorId, // 레거시 — 수용하되 무시 (ADR-0005)
            @Valid @RequestBody SpecialLectureCreateRequest request
    ) {
        SpecialLectureCreateCommand command = request.toCommand(memberId);
        SpecialLecture savedSpecialLecture = specialLectureService.createSpecialLecture(command);
        URI location = URI.create("/special-lectures/" + savedSpecialLecture.getId());
        return ResponseEntity
                .created(location)
                .body(SpecialLectureCreateResponse.from(savedSpecialLecture));
    }

    /**
     * 특강 목록 조회.
     *
     * <p>역할 제한은 없지만 <b>인증은 필요하다</b> — 무인증 허용은 {@code @PublicEndpoint}로만
     * 표현하며 이 컨트롤러에는 없다(ADR-0005). 일반 강의 조회 3종과 같은 규약이다.
     *
     * <p>생성·신청만 있고 목록이 없어서 웹이 이 화면을 자리표시자로 두고 있었다.
     */
    @GetMapping("/special-lectures")
    public ResponseEntity<SpecialLectureGetResponse> getSpecialLectures() {
        List<SpecialLecture> specialLectures = specialLectureService.findAllSpecialLectures();
        return ResponseEntity.ok(SpecialLectureGetResponse.from(specialLectures));
    }
}
