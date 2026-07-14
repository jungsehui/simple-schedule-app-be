package com.example.simplescheduleapp.integration;

import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.LectureService;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.application.port.out.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 인프로세스 어댑터 (ADR-0003 Stage 2) — notification의 {@link EnrolledStudentsPort}를
 * course 서비스 직접 호출로 구현한다. 단일 JVM이 되면서 기존 HTTP 자기호출({@code CourseClient} →
 * course의 {@code /internal/lectures/{id}/student-ids})을 제거하고, 같은 컨텍스트의 빈을 직접 주입한다.
 *
 * <p>합성 루트(:app)에 위치 — 두 바운디드 컨텍스트를 아는 유일한 지점. course/notification 모듈은
 * 여전히 서로를 컴파일 의존하지 않는다(notification은 포트만, course는 notification을 모름).
 */
@RequiredArgsConstructor
@Component
public class InProcessEnrolledStudentsAdapter implements EnrolledStudentsPort {

    private final LectureService lectureService;
    private final LectureEnrollmentService lectureEnrollmentService;

    @Override
    public GetEnrolledStudentInfosResponse getEnrolledStudentInfosByLectureId(Long lectureId) {
        Lecture lecture = lectureService.findLecture(lectureId);
        List<Long> studentIds = lectureEnrollmentService.findStudentIdsByLectureId(lectureId);
        return new GetEnrolledStudentInfosResponse(lecture.getTitle(), lecture.getMemo(), studentIds);
    }
}
