package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.result.LectureEnrollmentDetail;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.presentation.response.EnrolledLectureGetResponse;
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

    /**
     * 내 수강목록 — 학생 전용.
     *
     * <p><b>경로에 식별자가 없는 이유.</b> 조회 대상은 토큰 주체로만 정해진다. 대칭인
     * {@code GET /tutors/{tutorId}/lectures}는 경로 변수를 쓰지만, 그 모양은 ADR-0005가 SSE에서
     * 걷어낸 바로 그것이다 — 남의 식별자를 넣으면 남의 데이터가 나온다. 새로 내는 조회에
     * 같은 구멍을 다시 파지 않는다. (기존 tutors 경로 정리는 세 클라이언트 동시 릴리스가
     * 필요해 별도 결정으로 남긴다.)
     *
     * <p>아무것도 신청하지 않은 학생은 <b>빈 목록과 200</b>을 받는다. 첫 화면이 에러로 뜨면 안 된다.
     */
    @RequireRole(Role.STUDENT)
    @GetMapping("/me/lectures")
    public ResponseEntity<EnrolledLectureGetResponse> getMyLectures(@Auth Long memberId) {
        List<Lecture> lectures = lectureEnrollmentService.findEnrolledLectures(memberId);
        return ResponseEntity.ok(EnrolledLectureGetResponse.from(lectures));
    }
}
