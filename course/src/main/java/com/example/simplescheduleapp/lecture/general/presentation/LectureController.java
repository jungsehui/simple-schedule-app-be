package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.lecture.general.application.LectureService;
import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.general.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.presentation.request.LectureCreateRequest;
import com.example.simplescheduleapp.lecture.general.presentation.request.LectureUpdateRequest;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureCreateResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureSearchResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureUpdateResponse;
import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.general.presentation.response.TutorLectureGetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 강의 API.
 *
 * <p>조회 3종은 역할 제한이 없지만 <b>인증은 필요하다</b> — 무인증 허용은 {@code @PublicEndpoint}로만
 * 표현하며 이 컨트롤러에는 없다(ADR-0005). {@code tutorId} 쿼리 파라미터는 하위호환으로 남기되
 * 값을 쓰지 않는다.
 */
@RequiredArgsConstructor
@RestController
public class LectureController {

    private final LectureService lectureService;

    @RequireRole(Role.TUTOR)
    @PostMapping("/lectures")
    public ResponseEntity<LectureCreateResponse> createLecture(
            @Auth Long memberId,
            @RequestParam(required = false) Long tutorId, // 레거시 — 수용하되 무시
            @Valid @RequestBody LectureCreateRequest lectureCreateRequest
    ) {
        LectureCreateCommand command = lectureCreateRequest.toCommand(memberId);
        Lecture savedLecture = lectureService.createLecture(command);
        URI location = URI.create("/lectures/" + savedLecture.getId());
        return ResponseEntity
                .created(location)
                .body(LectureCreateResponse.from(savedLecture));
    }

    /**
     * 강의 상세.
     *
     * <p>목록과 <b>같은 항목 모양</b>({@link LectureResponse})을 준다. 종전에는 쓰기 응답인
     * {@code LectureCreateResponse}를 재사용해 {@code enrolledCount}가 빠져 있었다 — 목록에는
     * 잔여석이 보이는데 상세로 들어가면 사라졌다. 데이터가 없어서가 아니라 DTO가 버린 것이다.
     *
     * <p>쓰기 응답을 읽기가 재사용하면 계약이 몰래 묶인다. 생성 응답에 필드를 넣거나 빼면
     * 상세 조회 계약이 함께 움직이는데 알아차릴 방법이 없다. 타입을 갈라 그 결합을 끊는다.
     */
    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<LectureResponse> getLecture(@PathVariable Long lectureId) {
        Lecture lecture = lectureService.findLecture(lectureId);
        return ResponseEntity.ok(LectureResponse.from(lecture));
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
            @Auth Long memberId,
            @RequestParam(required = false) Long tutorId, // 레거시 — 수용하되 무시
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequest lectureUpdateRequest
    ) {
        LectureUpdateCommand command = lectureUpdateRequest.toCommand(memberId, lectureId);
        Lecture updatedLecture = lectureService.updateLecture(command);
        return ResponseEntity.ok(LectureUpdateResponse.from(updatedLecture));
    }
}
