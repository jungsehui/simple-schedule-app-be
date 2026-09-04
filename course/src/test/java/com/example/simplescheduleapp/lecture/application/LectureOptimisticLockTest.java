package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 낙관적 락(@Version) 왕복 검증 — ADR-0004 도메인/영속 모델 분리의 안전망.
 *
 * <p>순수화 이후 도메인은 detached 상태로 매퍼를 오간다. 매퍼가 version을 왕복시키지 않으면
 * merge 시 {@code WHERE version = ?}가 사라져 <b>낙관적 락이 조용히 무력화</b>된다
 * (특강 4단계 방어의 3차선, enrolledCount 동시 수정 감지). 기존 테스트는 @Retryable 애노테이션
 * 존재(계약)나 mock 배선만 확인해 이 회귀를 잡지 못하므로, 실제 DB 왕복으로 검증한다.
 *
 * <p>트랜잭션을 걸지 않아 각 리포지토리 호출이 자체 트랜잭션으로 커밋된다(= 서로 다른 사용자의
 * 동시 수정 시나리오 재현).
 */
@DisplayName("낙관적 락 왕복 (ADR-0004)")
class LectureOptimisticLockTest extends ApplicationTest {

    @Autowired
    LectureRepository lectureRepository;

    @Autowired
    TutorRepository tutorRepository;

    @Test
    void 순수_도메인_왕복_후에도_낡은_version의_수정은_낙관적_락으로_거부된다() {
        // given — 강의 저장
        Tutor tutor = tutorRepository.save(new Tutor(
                "optlock" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                "Password123!", "튜터", 30, "01000000000", 3));
        LocalDateTime now = LocalDateTime.now();
        Lecture saved = lectureRepository.save(
                new Lecture("강의", now, now.plusHours(1), "메모", tutor.getId(), 10));

        // 같은 row를 두 번 로드 → 매퍼가 만든 서로 다른 detached 도메인 객체(같은 version)
        Lecture first = lectureRepository.getByLectureId(saved.getId());
        Lecture second = lectureRepository.getByLectureId(saved.getId());
        assertThat(first.getVersion())
                .as("엔티티→도메인 왕복에서 version이 실려 와야 한다")
                .isNotNull()
                .isEqualTo(second.getVersion());

        // when — 첫 번째 수정이 커밋되며 version 증가
        first.update(tutor.getId(), new Schedule("수정1", now, now.plusHours(1), "메모1"), 11);
        Lecture updated = lectureRepository.save(first);

        assertThat(updated.getVersion())
                .as("수정 후 version이 증가해 도메인으로 돌아와야 한다(도메인→엔티티→DB 왕복 증거)")
                .isGreaterThan(saved.getVersion());

        // then — 낡은 version을 든 두 번째 수정은 거부되어야 한다
        second.update(tutor.getId(), new Schedule("수정2", now, now.plusHours(1), "메모2"), 12);
        assertThatThrownBy(() -> lectureRepository.save(second))
                .as("version 왕복이 끊기면 이 동시 수정이 조용히 덮어써진다")
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
