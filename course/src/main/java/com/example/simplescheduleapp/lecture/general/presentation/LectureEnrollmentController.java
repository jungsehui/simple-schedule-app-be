package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
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

    /**
     * 수강생 명단 조회 — 강사 전용이며, 소유권(자기 강의인가)은 서비스가 애그리거트에 위임해 검증한다.
     *
     * <p>이전에는 인증·인가가 전혀 없어 강의 ID만 알면 누구나 수강생 명단을 볼 수 있었다 (ADR-0005).
     */
    @RequireRole(Role.TUTOR)
    @GetMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentGetResponse> getLectureEnrollments(
            @Auth Long memberId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentDetail detail = lectureEnrollmentService.getLectureEnrollmentDetail(memberId, lectureId);
        List<StudentInfoResponse> students = StudentInfoResponse.from(detail.students());
        return ResponseEntity.ok(LectureEnrollmentGetResponse.of(detail.lecture(), students));
    }
}
