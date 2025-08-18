package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.application.LectureService;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.presentation.response.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class LectureEnrollmentInternalController {

    private final LectureService lectureService;
    private final LectureEnrollmentService lectureEnrollmentService;

    @GetMapping("/internal/lectures/{lectureId}/student-ids")
    public ResponseEntity<GetEnrolledStudentInfosResponse> getEnrolledStudentInfos(@PathVariable Long lectureId) {
        log.info("Try to get student infos by lecture id request. lecture id: {}", lectureId);
        Lecture lecture = lectureService.findLecture(lectureId);
        List<Long> studentIds = lectureEnrollmentService.findStudentIdsByLectureId(lectureId);
        log.info("Successfully to get student infos by lecture request. lecture id: {}", lectureId);
        return ResponseEntity.ok(GetEnrolledStudentInfosResponse.of(lecture, studentIds));
    }
}
