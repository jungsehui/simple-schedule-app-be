package com.example.simplescheduleapp.exception;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.CommonExceptionHandler;
import com.example.simplescheduleapp.common.exception.ExceptionCode;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.general.exception.PendingLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.schedule.exception.ScheduleExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * 예외 코드 → HTTP 상태 특성화 테스트 (course 소유 24개).
 *
 * <p>상태 코드는 클라이언트 노출 계약(API-CONTRACT.md)이다. 예외 코드 내부 표현이 바뀌어도
 * 핸들러를 통과한 최종 상태 코드는 변하면 안 된다.
 */
@DisplayName("예외 코드 HTTP 상태 계약(course) 은(는)")
class ExceptionCodeHttpContractTest {

    private final CommonExceptionHandler handler = new CommonExceptionHandler();

    @DisplayName("리팩터링 전과 동일한 상태 코드를 유지한다")
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("contract")
    void statusCodeContractIsPreserved(ExceptionCode code, HttpStatus expected) {
        assertThat(handler.handleApplicationException(new ApplicationException(code)).getStatusCode())
                .isEqualTo(expected);
    }

    static Stream<Arguments> contract() {
        return Stream.of(
                arguments(LectureExceptionCode.LECTURE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(LectureExceptionCode.INVALID_LECTURE_TIME_PAST, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.TUTOR_UNAUTHORIZED, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.ALREADY_ENROLLED, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.CAPACITY_EXCEEDED, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.ALREADY_REQUESTED, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.CAPACITY_UNDER_ZERO, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.CAPACITY_INFO_NOT_FOUND, HttpStatus.BAD_REQUEST),
                arguments(LectureExceptionCode.CAPACITY_BELOW_ENROLLED, HttpStatus.BAD_REQUEST),
                arguments(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(PendingLectureEnrollmentExceptionCode.ALREADY_CANCELED, HttpStatus.BAD_REQUEST),
                arguments(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS, HttpStatus.NOT_FOUND),
                arguments(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR),
                arguments(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED, HttpStatus.CONFLICT),
                arguments(ScheduleExceptionCode.TUTOR_SCHEDULE_CONFLICT, HttpStatus.CONFLICT),
                arguments(ScheduleExceptionCode.STUDENT_SCHEDULE_CONFLICT, HttpStatus.CONFLICT),
                arguments(MemberExceptionCode.INVALID_USERNAME_PASSWORD, HttpStatus.UNAUTHORIZED),
                arguments(MemberExceptionCode.DUPLICATED_USERNAME_PHONE, HttpStatus.CONFLICT),
                arguments(MemberExceptionCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(MemberExceptionCode.TUTOR_NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(MemberExceptionCode.STUDENT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
