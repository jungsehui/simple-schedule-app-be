package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.result.LectureEnrollmentDetail;
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
        LectureEnrollmentDetail detail = lectureEnrollmentService.getLectureEnrollmentDetail(lectureId);
        List<StudentInfoResponse> students = StudentInfoResponse.from(detail.students());
        return ResponseEntity.ok(LectureEnrollmentGetResponse.of(detail.lecture(), students));
    }
}
