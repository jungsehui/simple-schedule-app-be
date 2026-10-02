package com.example.simplescheduleapp.notification.application.port.out;

/**
 * 아웃바운드 포트: 강의 수강생 조회 (구현: {@code :app}의 인프로세스 어댑터
 * {@code InProcessEnrolledStudentsAdapter}. 예전 HTTP 어댑터 {@code CourseClient}는 단일 JVM 전환 때 제거됨).
 *
 * <p>알림 전략은 이 포트에만 의존한다 — course 서버와의 통신 수단(HTTP/gRPC/캐시)이
 * 바뀌어도 유스케이스 코드는 불변이다. (헥사고날 규약: docs/architecture/hexagonal-guidelines.md)
 */
public interface EnrolledStudentsPort {

    GetEnrolledStudentInfosResponse getEnrolledStudentInfosByLectureId(Long lectureId);
}
