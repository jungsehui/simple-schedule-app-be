package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.presentation.request.LectureEnrollmentCreateRequest;
import com.example.simplescheduleapp.lecture.presentation.response.LectureEnrollmentCreateResponse;
import com.example.simplescheduleapp.lecture.presentation.response.LectureEnrollmentGetResponse;
import com.example.simplescheduleapp.lecture.presentation.response.StudentInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class LectureEnrollmentController {

    private final LectureEnrollmentService lectureEnrollmentService;

    @PostMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> requestLectureEnrollment(
            @PathVariable Long lectureId,
            @RequestParam Long studentId
    ) {
        LectureEnrollmentCreateCommand command = LectureEnrollmentCreateCommand.of(studentId, lectureId);
        lectureEnrollmentService.requestEnrollment(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentGetResponse> getLectureEnrollments(
            @PathVariable Long lectureId
    ) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentService.getLectureEnrollments(lectureId);
        List<StudentInfoResponse> students = StudentInfoResponse.from(lectureEnrollments);
        return ResponseEntity.ok(LectureEnrollmentGetResponse.of(lectureEnrollments, students));
    }

    @DeleteMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> cancelEnrollment(
            @RequestParam Long studentId,
            @PathVariable Long lectureId
    ) {
        lectureEnrollmentService.cancelEnrollment(studentId, lectureId);
        return ResponseEntity.noContent().build();
    }
}
