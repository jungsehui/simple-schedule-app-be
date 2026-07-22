package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * pending 하드 삭제 회귀 가드 (main {@code 963379f}의 테스트를 순수화 구조로 이식 —
 * 원본은 course에 없는 {@code support.ApplicationTest}에 의존했다).
 *
 * <p>의도적으로 {@code @Transactional} 없이 실행한다 — 같은 트랜잭션 안에서는 Hibernate가
 * flush 시 INSERT를 DELETE보다 먼저 실행해, 실제 운영 경로(취소 커밋 후 재신청)와 다른
 * 순서로 유니크 제약을 평가하기 때문이다. 포트 호출 각각이 자체 트랜잭션으로 커밋된다.
 */
@SpringBootTest
class PendingLectureEnrollmentRepositoryTest {

    @Autowired
    private PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    @Test
    void 삭제된_수강신청과_같은_강의_학생_조합으로_다시_신청할_수_있다() {
        // given: 같은 (강의, 학생) 조합의 신청이 저장됐다가 취소(삭제)된 상태
        PendingLectureEnrollment first =
                pendingLectureEnrollmentRepository.save(new PendingLectureEnrollment(991L, 991L));
        pendingLectureEnrollmentRepository.delete(first);

        // when: 같은 조합으로 다시 신청한다
        // (소프트 삭제라면 남아 있는 행이 uk_pending_lecture_student와 충돌해 여기서 실패한다)
        PendingLectureEnrollment second =
                pendingLectureEnrollmentRepository.save(new PendingLectureEnrollment(991L, 991L));

        // then
        assertThat(second.getId()).isNotNull();
    }
}
