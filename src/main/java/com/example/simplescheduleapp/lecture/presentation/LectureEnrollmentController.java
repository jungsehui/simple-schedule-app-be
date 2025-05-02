package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
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
    public ResponseEntity<LectureEnrollmentCreateResponse> enrollStudent(
            @RequestParam Long studentId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCreateCommand lectureEnrollmentCreateCommand = LectureEnrollmentCreateCommand.of(studentId, lectureId);
        LectureEnrollment lectureEnrollment = lectureEnrollmentService.enroll(lectureEnrollmentCreateCommand);
        URI location = URI.create("/lectures/" + lectureId + "/enrollments/" + lectureEnrollment.getId());
        return ResponseEntity
                .created(location)
                .body(LectureEnrollmentCreateResponse.from(lectureEnrollment));
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
            @PathVariable Long lectureId,
            @RequestParam Long studentId
    ) {
        lectureEnrollmentService.cancelEnrollment(lectureId, studentId);
        return ResponseEntity.noContent().build();
    }
}
