package com.example.simplescheduleapp.lecture.general.application;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@code findEnrolledLectures}의 조회 조립을 검증한다.
 *
 * <p><b>왜 HTTP 테스트로 부족한가.</b> {@code EnrolledLectureHttpTest}는 서비스를 목하므로
 * 서비스 <em>안쪽</em>에서 식별자를 잘못 넘겨도 초록이다. 실제로 이 테스트를 쓰기 전에
 * {@code findAllByStudentId(studentId + 1)}로 변이시켜 봤더니 HTTP 테스트 4개가 전부 통과했다.
 * 같은 부류(인자 자리를 잘못 넘기는 조용한 버그)가 이 프로젝트에서 이미 한 번 나왔다.
 */
@DisplayName("내 수강목록 조회(서비스) 은(는)")
class EnrolledLectureQueryTest extends UnitTest {

    @InjectMocks
    private LectureEnrollmentService lectureEnrollmentService;

    @Mock
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Mock
    private LectureRepository lectureRepository;

    @DisplayName("요청받은 학생의 수강등록만 읽고, 그 강의 ID들로 강의를 한 번에 가져온다")
    @Test
    void 학생의_수강등록에서_강의ID를_모아_한_번에_조회한다() {
        Long studentId = 42L;
        Long otherStudentId = 43L;
        doReturn(List.of(
                LectureEnrollment.reconstitute(1L, 100L, studentId),
                LectureEnrollment.reconstitute(2L, 200L, studentId)))
                .when(lectureEnrollmentRepository).findAllByStudentId(studentId);

        lectureEnrollmentService.findEnrolledLectures(studentId);

        // 1) 다른 학생의 수강등록을 읽지 않는다
        verify(lectureEnrollmentRepository, never()).findAllByStudentId(otherStudentId);

        // 2) 강의는 한 번에(N+1 아님), 수강등록에서 뽑은 ID 그대로
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(lectureRepository).findAllByIdsOrderByStartTime(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(100L, 200L);
    }

    @DisplayName("수강등록이 없으면 강의 조회를 아예 하지 않고 빈 목록을 준다")
    @Test
    void 수강등록이_없으면_강의를_조회하지_않는다() {
        Long studentId = 44L;
        doReturn(List.of()).when(lectureEnrollmentRepository).findAllByStudentId(studentId);
        doReturn(List.of()).when(lectureRepository).findAllByIdsOrderByStartTime(List.of());

        List<Lecture> lectures = lectureEnrollmentService.findEnrolledLectures(studentId);

        assertThat(lectures).isEmpty();
    }

    @DisplayName("강의 목록은 리포지터리가 준 순서(시작 시각 오름차순)를 그대로 유지한다")
    @Test
    void 리포지터리가_준_순서를_유지한다() {
        Long studentId = 45L;
        Lecture earlier = new Lecture("1교시", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), "", 9L, 10);
        Lecture later = new Lecture("2교시", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1), "", 9L, 10);
        doReturn(List.of(LectureEnrollment.reconstitute(1L, 100L, studentId)))
                .when(lectureEnrollmentRepository).findAllByStudentId(studentId);
        doReturn(List.of(earlier, later)).when(lectureRepository).findAllByIdsOrderByStartTime(anyList());

        List<Lecture> lectures = lectureEnrollmentService.findEnrolledLectures(studentId);

        assertThat(lectures).extracting(Lecture::getTitle).containsExactly("1교시", "2교시");
    }
}
