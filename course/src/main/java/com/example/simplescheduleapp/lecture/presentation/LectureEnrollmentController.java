package com.example.simplescheduleapp.lecture.presentation;

import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.application.LectureService;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.presentation.response.LectureEnrollmentGetResponse;
import com.example.simplescheduleapp.lecture.presentation.response.EnrolledStudentInfosResponse;
import com.example.simplescheduleapp.lecture.presentation.response.StudentInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class LectureEnrollmentController {

    private final LectureService lectureService;
    private final LectureEnrollmentService lectureEnrollmentService;

    @GetMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentGetResponse> getLectureEnrollments(@PathVariable Long lectureId) {
        List<LectureEnrollment> lectureEnrollments = lectureEnrollmentService.getLectureEnrollments(lectureId);
        List<StudentInfoResponse> studentInfos = StudentInfoResponse.from(lectureEnrollments);
        return ResponseEntity.ok(LectureEnrollmentGetResponse.of(lectureEnrollments, studentInfos));
    }

    @GetMapping("/internal/lectures/{lectureId}/student-ids")
    public ResponseEntity<EnrolledStudentInfosResponse> getEnrolledStudentInfos(@PathVariable Long lectureId) {
        Lecture lecture = lectureService.findLecture(lectureId);
        List<Long> studentIds = lectureEnrollmentService.findStudentIdsByLectureId(lectureId);
        return ResponseEntity.ok(EnrolledStudentInfosResponse.of(lecture, studentIds));
    }
}
