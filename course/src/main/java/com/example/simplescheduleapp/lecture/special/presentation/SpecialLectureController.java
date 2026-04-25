package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.lecture.special.application.SpecialLectureService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.presentation.request.SpecialLectureCreateRequest;
import com.example.simplescheduleapp.lecture.special.presentation.response.SpecialLectureCreateResponse;
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

    @PostMapping("/special-lectures")
    public ResponseEntity<SpecialLectureCreateResponse> createSpecialLecture(
            @RequestParam Long tutorId,
            @RequestBody SpecialLectureCreateRequest request
    ) {
        SpecialLectureCreateCommand command = request.toCommand(tutorId);
        SpecialLecture savedSpecialLecture = specialLectureService.createSpecialLecture(command);
        URI location = URI.create("/special-lectures/" + savedSpecialLecture.getId());
        return ResponseEntity
                .created(location)
                .body(SpecialLectureCreateResponse.from(savedSpecialLecture));
    }
}
