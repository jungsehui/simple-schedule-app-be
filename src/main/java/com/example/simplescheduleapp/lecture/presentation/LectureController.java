package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureService;
import com.example.simplescheduleapp.lecture.presentation.request.LectureCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LectureController {

    private final LectureService lectureService;

    @PostMapping("/lectures")
    public ResponseEntity<Void> createLecture(
            Long memberId,
            @RequestBody LectureCreateRequest lectureCreateRequest
    ) {
        return null;
    }
}
