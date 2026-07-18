package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PendingLectureEnrollmentRepositoryTest extends ApplicationTest {

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
        assertThat(pendingLectureEnrollmentRepository.existsByLectureIdAndStudentId(991L, 991L)).isTrue();
    }
}
