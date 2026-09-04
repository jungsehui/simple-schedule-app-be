package com.example.simplescheduleapp.lecture.special.exception;

import com.example.simplescheduleapp.common.exception.ExceptionCode;
import lombok.Getter;
import com.example.simplescheduleapp.common.exception.ErrorKind;

@Getter
public enum SpecialLectureEnrollmentExceptionCode implements ExceptionCode {

    SPECIAL_LECTURE_ENROLLMENT_FAILED(ErrorKind.INTERNAL_SERVER_ERROR, "SLE001", "수강신청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    SPECIAL_LECTURE_NOT_FOUND(ErrorKind.NOT_FOUND, "SLE002", "해당 특강을 찾을 수 없습니다."),
    ALREADY_ENROLLED(ErrorKind.CONFLICT, "SLE003", "이미 수강신청이 완료된 특강입니다."),

    /**
     * 정원 소진. <b>409다.</b>
     *
     * <p>종전에는 일반 강의의 {@code LectureExceptionCode.CAPACITY_EXCEEDED}(L4)를 던졌고
     * 그것은 <b>400</b>이었다. 두 가지가 잘못이었다.
     *
     * <p>첫째, 400은 "보낸 값이 잘못됐으니 고쳐서 다시 보내라"는 뜻이다. 만석은 완벽히
     * 유효한 요청에 대해 <b>서버 상태가 거부</b>한 것이라 클라이언트가 고칠 입력이 없다.
     * 상태 충돌이므로 409다.
     *
     * <p>둘째, {@code /special-lectures/**}가 일반 {@code Lecture} 네임스페이스 코드를
     * 반환하고 있었다. 특강 전용 코드를 두면 {@code L4}의 상태 코드를 건드리지 않고
     * 특강만 정확해진다 — 일반 강의 계약을 동반 변경하지 않는다.
     */
    CAPACITY_EXCEEDED(ErrorKind.CONFLICT, "SLE004", "특강 정원이 모두 찼습니다."),

    /**
     * 이미 끝난 특강. <b>409다.</b> 만석과 같은 이유로, 유효한 요청에 대한 상태 거부다.
     *
     * <p>기준은 {@code endTime}이다. 진행 중 합류는 허용한다(오너 결정 2026-09-04).
     *
     * <p>종전에는 신청 경로에 시간 비교가 <b>0건</b>이라 끝난 특강도 신청을 받았다.
     * 목록에 날짜 필터가 없어 지난 특강이 맨 위에 뜨는 것과 겹치면
     * "지난달 특강 + 동작하는 신청 버튼"이 된다.
     */
    SPECIAL_LECTURE_ENDED(ErrorKind.CONFLICT, "SLE005", "이미 종료된 특강입니다."),

    /**
     * 취소할 신청이 없다. 404다.
     *
     * <p>이미 취소했거나 애초에 신청하지 않은 경우다. <b>두 번째 취소 요청도 여기로 온다</b> —
     * 삭제가 0행을 반환하기 때문이다. 그래서 좌석이 두 번 반환되지 않는다.
     */
    ENROLLMENT_NOT_FOUND(ErrorKind.NOT_FOUND, "SLE006", "취소할 특강 수강신청이 없습니다."),
    ;

    private final ErrorKind kind;
    private final String code;
    private final String message;

    SpecialLectureEnrollmentExceptionCode(ErrorKind kind, String code, String message) {
        this.kind = kind;
        this.code = code;
        this.message = message;
    }
}
