package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureService;
import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.lecture.presentation.request.LectureCreateRequest;
import com.example.simplescheduleapp.lecture.presentation.request.LectureUpdateRequest;
import com.example.simplescheduleapp.lecture.presentation.response.LectureCreateResponse;
import com.example.simplescheduleapp.lecture.presentation.response.LectureSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class LectureController {

    private final LectureService lectureService;

    @PostMapping("/lectures")
    public ResponseEntity<LectureCreateResponse> createLecture(
            @RequestParam Long memberId,
            @RequestBody LectureCreateRequest lectureCreateRequest
    ) {
        LectureCreateCommand lectureCreateCommand = lectureCreateRequest.toCommand(memberId);
        Lecture savedLecture = lectureService.createLecture(lectureCreateCommand);
        URI location = URI.create("/lectures/" + savedLecture.getId());
        return ResponseEntity
                .created(location)
                .body(LectureCreateResponse.from(savedLecture));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<LectureCreateResponse> getLecture(@PathVariable Long lectureId) {
        Lecture lecture = lectureService.getLecture(lectureId);
        return ResponseEntity.ok(LectureCreateResponse.from(lecture));
    }

    @GetMapping("/tutors/lectures/{tutorId}")
    public ResponseEntity<LectureSearchResponse> getLecturesByTutorId(@PathVariable Long tutorId) {
        List<Lecture> lectures = lectureService.findLectures(tutorId);
        return ResponseEntity.ok(LectureSearchResponse.from(lectures));
    }

    @GetMapping("/search")
    public ResponseEntity<LectureSearchResponse> searchLecturesByKeyword(@RequestParam String keyword) {
        List<Lecture> lectures = lectureService.searchLecturesByKeyword(keyword);
        return ResponseEntity.ok(LectureSearchResponse.from(lectures));
    }

    @PatchMapping("/lectures/{lectureId}")
    public ResponseEntity<Void> updateLecture(
            @PathVariable Long lectureId,
            @RequestBody LectureUpdateRequest lectureUpdateRequest
    ) {
        LectureUpdateCommand lectureUpdateCommand = lectureUpdateRequest.toCommand(lectureId);
        lectureService.updateLecture(lectureUpdateCommand);
        return ResponseEntity.noContent().build();
    }
}
