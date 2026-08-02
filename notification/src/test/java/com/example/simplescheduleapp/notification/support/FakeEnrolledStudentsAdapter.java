package com.example.simplescheduleapp.notification.support;

import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.application.port.out.GetEnrolledStudentInfosResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 테스트 전용 {@link EnrolledStudentsPort} 스텁 (ADR-0003 Stage 2).
 * <p>
 * 프로덕션 어댑터(HTTP {@code CourseClient})는 제거되고 인프로세스 어댑터는 :app에 있으므로,
 * notification 모듈의 격리된 통합 테스트 컨텍스트가 부팅되도록 이 기본 빈을 test 스코프로 제공한다.
 * 실제 값을 검증하는 테스트는 {@code @MockitoBean EnrolledStudentsPort}로 이 빈을 덮어쓴다.
 */
@Component
public class FakeEnrolledStudentsAdapter implements EnrolledStudentsPort {

    @Override
    public GetEnrolledStudentInfosResponse getEnrolledStudentInfosByLectureId(Long lectureId) {
        return new GetEnrolledStudentInfosResponse("", "", List.of());
    }
}
