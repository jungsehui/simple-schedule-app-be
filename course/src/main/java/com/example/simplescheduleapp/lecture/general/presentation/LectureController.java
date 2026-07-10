package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.lecture.general.application.LectureService;
import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.general.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.presentation.request.LectureCreateRequest;
import com.example.simplescheduleapp.lecture.general.presentation.request.LectureUpdateRequest;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureCreateResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureSearchResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureUpdateResponse;
import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.general.presentation.response.TutorLectureGetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class LectureController {

    private final LectureService lectureService;

    // Phase 3a 듀얼리드: 토큰 식별자 우선, tutorId 파라미터는 레거시 폴백 (3b에서 제거 예정)
    @RequireRole(Role.TUTOR)
    @PostMapping("/lectures")
    public ResponseEntity<LectureCreateResponse> createLecture(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long tutorId,
            @Valid @RequestBody LectureCreateRequest lectureCreateRequest
    ) {
        LectureCreateCommand command = lectureCreateRequest.toCommand(AuthIdentities.resolve(memberId, tutorId));
        Lecture savedLecture = lectureService.createLecture(command);
        URI location = URI.create("/lectures/" + savedLecture.getId());
        return ResponseEntity
                .created(location)
                .body(LectureCreateResponse.from(savedLecture));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<LectureCreateResponse> getLecture(@PathVariable Long lectureId) {
        Lecture lecture = lectureService.findLecture(lectureId);
        return ResponseEntity.ok(LectureCreateResponse.from(lecture));
    }

    @GetMapping("/tutors/{tutorId}/lectures")
    public ResponseEntity<TutorLectureGetResponse> getTutorLectures(@PathVariable Long tutorId) {
        List<Lecture> tutorLectures = lectureService.findAllTutorLectures(tutorId);
        return ResponseEntity.ok(TutorLectureGetResponse.from(tutorLectures));
    }

    @GetMapping("/lectures/search")
    public ResponseEntity<LectureSearchResponse> searchLectures(@RequestParam String keyword) {
        List<Lecture> lectures = lectureService.searchLectures(keyword);
        return ResponseEntity.ok(LectureSearchResponse.from(lectures));
    }

    @RequireRole(Role.TUTOR)
    @PatchMapping("/lectures/{lectureId}")
    public ResponseEntity<LectureUpdateResponse> updateLecture(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long tutorId,
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequest lectureUpdateRequest
    ) {
        LectureUpdateCommand command = lectureUpdateRequest.toCommand(AuthIdentities.resolve(memberId, tutorId), lectureId);
        Lecture updatedLecture = lectureService.updateLecture(command);
        return ResponseEntity.ok(LectureUpdateResponse.from(updatedLecture));
    }
}
