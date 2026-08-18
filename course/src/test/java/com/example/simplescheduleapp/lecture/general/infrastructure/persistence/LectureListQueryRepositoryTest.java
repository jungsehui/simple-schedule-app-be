package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 목록 조회 두 건의 <b>정렬과 경계</b>를 실제 DB로 고정한다.
 *
 * <p>정렬은 두 포트의 문서에 계약으로 적어 뒀지만, 적어 두는 것과 지켜지는 것은 다르다.
 * 어댑터에서 정렬 없는 {@code findAll()}로 바꿔도 상위 테스트(서비스·HTTP)는 전부 초록이었다 —
 * 목을 쓰기 때문이다. 이 테스트가 그 구간을 덮는다.
 *
 * <p>{@code @Transactional}로 롤백해 다른 테스트의 데이터와 섞이지 않게 한다. 그럼에도
 * 전체 목록을 반환하는 특강 조회는 오염 가능성이 있어, 삽입한 항목만 골라 <b>상대 순서</b>를 본다.
 */
@DisplayName("강의 목록 조회 쿼리 은(는)")
@SpringBootTest
@Transactional
class LectureListQueryRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2031, 3, 1, 9, 0);

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private SpecialLectureRepository specialLectureRepository;

    @DisplayName("ID 묶음 조회는 시작 시각 오름차순이다 — 넘긴 ID 순서가 아니다")
    @Test
    void ID_묶음_조회는_시작시각_오름차순이다() {
        // given: 늦은 것을 먼저 저장한다. ID 순서와 시간 순서가 어긋나야 정렬을 검증할 수 있다.
        Lecture late = lectureRepository.save(newLecture("늦은 수업", BASE.plusHours(6)));
        Lecture early = lectureRepository.save(newLecture("이른 수업", BASE));
        Lecture middle = lectureRepository.save(newLecture("중간 수업", BASE.plusHours(3)));

        // when: 저장 순서(늦음→이름→중간) 그대로 ID를 넘긴다
        List<Lecture> found = lectureRepository.findAllByIdsOrderByStartTime(
                List.of(late.getId(), early.getId(), middle.getId()));

        // then: 넘긴 순서가 아니라 시작 시각 순서로 돌아온다
        assertThat(found).extracting(Lecture::getTitle)
                .containsExactly("이른 수업", "중간 수업", "늦은 수업");
    }

    @DisplayName("ID가 비어 있으면 예외 없이 빈 목록을 준다")
    @Test
    void 빈_ID_목록은_예외_없이_빈_결과다() {
        // 수강신청이 하나도 없는 학생의 첫 화면이 정확히 이 경로다.
        // 어댑터의 조기 반환 가드를 빼도 이 테스트는 통과한다(Hibernate가 빈 IN을 처리한다).
        // 즉 이 테스트가 지키는 것은 "가드가 있다"가 아니라 "빈 입력에 200과 빈 목록"이라는 계약이다.
        assertThatCode(() -> assertThat(lectureRepository.findAllByIdsOrderByStartTime(List.of())).isEmpty())
                .doesNotThrowAnyException();
    }

    @DisplayName("특강 목록은 시작 시각 오름차순이다")
    @Test
    void 특강_목록은_시작시각_오름차순이다() {
        // given: 늦은 것을 먼저 저장한다
        specialLectureRepository.save(newSpecialLecture("ZZ-늦은 특강", BASE.plusHours(6)));
        specialLectureRepository.save(newSpecialLecture("ZZ-이른 특강", BASE));

        // when
        List<SpecialLecture> found = specialLectureRepository.findAllOrderByStartTime();

        // then: 다른 테스트가 남긴 특강과 섞일 수 있으므로 이번에 넣은 것만 골라 상대 순서를 본다
        assertThat(found).extracting(SpecialLecture::getTitle)
                .filteredOn(title -> title.startsWith("ZZ-"))
                .containsExactly("ZZ-이른 특강", "ZZ-늦은 특강");
    }

    private Lecture newLecture(String title, LocalDateTime startTime) {
        return new Lecture(title, startTime, startTime.plusHours(1), "메모", 9001L, 20);
    }

    private SpecialLecture newSpecialLecture(String title, LocalDateTime startTime) {
        return new SpecialLecture(title, startTime, startTime.plusHours(1), "메모", 9001L, 30);
    }
}
