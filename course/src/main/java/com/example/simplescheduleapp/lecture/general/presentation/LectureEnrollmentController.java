package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.presentation.response.LectureEnrollmentGetResponse;
import com.example.simplescheduleapp.lecture.general.presentation.response.StudentInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class LectureEnrollmentController {

    private final LectureEnrollmentService lectureEnrollmentService;

    @GetMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentGetResponse> getLectureEnrollments(
            @PathVariable Long lectureId
    ) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentService.getLectureEnrollments(lectureId);
        List<StudentInfoResponse> students = StudentInfoResponse.from(lectureEnrollments);
        return ResponseEntity.ok(LectureEnrollmentGetResponse.of(lectureEnrollments, students));
    }
}
