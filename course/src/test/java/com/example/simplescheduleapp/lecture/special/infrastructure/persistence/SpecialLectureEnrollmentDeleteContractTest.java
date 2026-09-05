package com.example.simplescheduleapp.lecture.special.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.application.SpecialLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 삭제가 <b>영향 행 수를 정확히 알려 주는지</b>를 실제 DB에서 고정한다.
 *
 * <p><b>왜 목으로는 안 되는가.</b> 취소의 멱등성은 "두 번째 삭제는 0행"이라는 DB 계약 위에
 * 서 있다. 그 계약을 목으로 스텁하면 우리가 지시한 값을 확인하는 꼴이라, 파생 삭제 메서드로
 * 바꾸거나 반환 타입을 {@code void}로 바꿔도 통과한다. 좌석 이중 반환을 막는 근거가 실제로
 * 성립하는지는 DB에 물어야 안다.
 */
@DisplayName("특강 신청 삭제 계약 은(는)")
class SpecialLectureEnrollmentDeleteContractTest extends ApplicationTest {

    private static final Long SPECIAL_LECTURE_ID = 8100L;
    private static final Long STUDENT_ID = 8200L;

    @Autowired
    private SpecialLectureEnrollmentRepository repository;

    /**
     * 삭제를 <b>서비스를 통해</b> 부른다. 벌크 삭제는 활성 트랜잭션을 요구하고, 프로덕션에서
     * 그 경계를 제공하는 것이 이 서비스다. 리포지토리를 직접 부르면 트랜잭션 없는 경로를
     * 검증하는 셈이라 실제로 쓰이지 않는 조건을 고정하게 된다.
     */
    @Autowired
    private SpecialLectureEnrollmentService enrollmentService;

    private int cancel(Long specialLectureId, Long studentId) {
        return enrollmentService.cancelSpecialLectureEnrollment(specialLectureId, studentId);
    }

    @DisplayName("있는 신청을 지우면 1을 반환한다")
    @Test
    void 있는_신청은_1을_반환한다() {
        repository.save(new SpecialLectureEnrollment(SPECIAL_LECTURE_ID, STUDENT_ID));

        int deleted = cancel(SPECIAL_LECTURE_ID, STUDENT_ID);

        assertThat(deleted).isEqualTo(1);
    }

    /**
     * <b>이것이 좌석 이중 반환을 막는 근거다.</b> 두 번째 취소가 0을 받아야 호출자가
     * "돌려줄 좌석이 없다"고 판단할 수 있다. 여기서 1이 나오면 취소 한 번에 좌석 두 개가 풀린다.
     */
    @DisplayName("이미 지운 신청을 다시 지우면 0을 반환한다")
    @Test
    void 두_번째_삭제는_0을_반환한다() {
        repository.save(new SpecialLectureEnrollment(SPECIAL_LECTURE_ID + 1, STUDENT_ID));
        cancel(SPECIAL_LECTURE_ID + 1, STUDENT_ID);

        int second = cancel(SPECIAL_LECTURE_ID + 1, STUDENT_ID);

        assertThat(second).isZero();
    }

    @DisplayName("애초에 신청하지 않았으면 0을 반환한다")
    @Test
    void 신청이_없으면_0을_반환한다() {
        int deleted = cancel(SPECIAL_LECTURE_ID + 2, STUDENT_ID);

        assertThat(deleted).isZero();
    }

    /**
     * 같은 특강에 다른 학생이 신청해 있어도 내 취소가 그 행을 건드리면 안 된다.
     * 삭제 조건이 두 컬럼 모두를 보는지 확인한다.
     */
    @DisplayName("다른 학생의 신청은 지우지 않는다")
    @Test
    void 남의_신청은_지우지_않는다() {
        Long lectureId = SPECIAL_LECTURE_ID + 3;
        repository.save(new SpecialLectureEnrollment(lectureId, STUDENT_ID));
        repository.save(new SpecialLectureEnrollment(lectureId, STUDENT_ID + 1));

        int deleted = cancel(lectureId, STUDENT_ID);

        assertThat(deleted).isEqualTo(1);
        // 남의 신청은 그대로 남아 있어야 하므로, 그것을 지우면 다시 1이 나온다
        assertThat(cancel(lectureId, STUDENT_ID + 1)).isEqualTo(1);
    }
}
