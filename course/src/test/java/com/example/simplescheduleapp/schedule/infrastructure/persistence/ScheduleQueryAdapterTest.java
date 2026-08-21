package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleQueryPort;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleType;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캘린더 네이티브 질의를 <b>실제 DB로</b> 검증한다.
 *
 * <p>이 테스트가 이 기능에서 가장 중요하다. 질의가 네이티브 SQL이라 컴파일러도 목(mock)도
 * 아무것도 잡아 주지 않는다 — 테이블명 오타, JOIN 컬럼 착오, UNION 뒤 ORDER BY 문법,
 * 인터페이스 프로젝션 컬럼 매핑이 전부 런타임에만 드러난다. 이 저장소는 이미 네이티브 질의
 * 하나가 엔진 차이로 깨진 적이 있다({@code 6ccf4a9}, MySQL의 CAST BIGINT).
 *
 * <p><b>남은 위험을 명시한다.</b> 테스트는 H2에서 돈다. 운영은 PostgreSQL, 로컬 개발은
 * MySQL이다. 여기 초록은 "H2에서 문법과 매핑이 맞다"까지만 보장한다.
 */
@DisplayName("캘린더 조회 질의 은(는)")
@SpringBootTest
@Transactional
class ScheduleQueryAdapterTest {

    private static final LocalDateTime WINDOW_FROM = LocalDateTime.of(2032, 5, 1, 0, 0);
    private static final LocalDateTime WINDOW_TO = LocalDateTime.of(2032, 5, 31, 23, 59);
    private static final Long TUTOR_ID = 77001L;
    private static final Long STUDENT_ID = 77002L;

    @Autowired
    private ScheduleQueryPort scheduleQueryPort;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private SpecialLectureRepository specialLectureRepository;

    @Autowired
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Autowired
    private SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;

    @DisplayName("강사 일정은 자기가 여는 강의와 특강을 종류와 함께 시작 시각 순으로 준다")
    @Test
    void 강사_일정은_강의와_특강을_종류와_함께_준다() {
        specialLectureRepository.save(specialLecture("5월 특강", WINDOW_FROM.plusDays(10)));
        lectureRepository.save(lecture("5월 강의", WINDOW_FROM.plusDays(3)));

        List<ScheduleView> views = scheduleQueryPort.findTutorSchedules(TUTOR_ID, WINDOW_FROM, WINDOW_TO);

        assertThat(views).extracting(ScheduleView::title)
                .containsExactly("5월 강의", "5월 특강");
        assertThat(views).extracting(ScheduleView::type)
                .containsExactly(ScheduleType.LECTURE, ScheduleType.SPECIAL_LECTURE);
        // 프로젝션이 실제로 값을 채웠는지 — 컬럼 매핑이 어긋나면 여기서 null이 된다
        assertThat(views).allSatisfy(v -> {
            assertThat(v.scheduleId()).isNotNull();
            assertThat(v.startTime()).isNotNull();
            assertThat(v.endTime()).isNotNull();
        });
    }

    @DisplayName("학생 일정은 신청한 강의와 특강을 UNION으로 합쳐 시작 시각 순으로 준다")
    @Test
    void 학생_일정은_두_수강신청을_합친다() {
        Lecture savedLecture = lectureRepository.save(lecture("학생 강의", WINDOW_FROM.plusDays(5)));
        SpecialLecture savedSpecial = specialLectureRepository.save(specialLecture("학생 특강", WINDOW_FROM.plusDays(2)));
        lectureEnrollmentRepository.save(new LectureEnrollment(savedLecture.getId(), STUDENT_ID));
        specialLectureEnrollmentRepository.save(new SpecialLectureEnrollment(savedSpecial.getId(), STUDENT_ID));

        List<ScheduleView> views = scheduleQueryPort.findStudentSchedules(STUDENT_ID, WINDOW_FROM, WINDOW_TO);

        assertThat(views).extracting(ScheduleView::title)
                .containsExactly("학생 특강", "학생 강의");
    }

    @DisplayName("신청하지 않은 강의는 학생 일정에 나오지 않는다")
    @Test
    void 신청하지_않은_강의는_안_나온다() {
        lectureRepository.save(lecture("남의 강의", WINDOW_FROM.plusDays(4)));

        List<ScheduleView> views = scheduleQueryPort.findStudentSchedules(STUDENT_ID, WINDOW_FROM, WINDOW_TO);

        assertThat(views).extracting(ScheduleView::title).doesNotContain("남의 강의");
    }

    @DisplayName("창에 걸친 일정도 포함한다 — 캘린더 왼쪽 끝에 구멍이 생기면 안 된다")
    @Test
    void 창에_걸친_일정도_포함한다() {
        // 창 시작 1시간 전에 시작해 창 안까지 이어지는 일정
        lectureRepository.save(lecture("걸친 강의", WINDOW_FROM.minusHours(1)));

        List<ScheduleView> views = scheduleQueryPort.findTutorSchedules(TUTOR_ID, WINDOW_FROM, WINDOW_TO);

        assertThat(views).extracting(ScheduleView::title).contains("걸친 강의");
    }

    @DisplayName("창 밖의 일정은 제외한다")
    @Test
    void 창_밖_일정은_제외한다() {
        lectureRepository.save(lecture("다음 달 강의", WINDOW_TO.plusDays(10)));

        List<ScheduleView> views = scheduleQueryPort.findTutorSchedules(TUTOR_ID, WINDOW_FROM, WINDOW_TO);

        assertThat(views).extracting(ScheduleView::title).doesNotContain("다음 달 강의");
    }

    @DisplayName("일정이 없으면 예외가 아니라 빈 목록이다")
    @Test
    void 일정이_없으면_빈_목록이다() {
        assertThat(scheduleQueryPort.findParentSchedules(99999L, WINDOW_FROM, WINDOW_TO)).isEmpty();
        assertThat(scheduleQueryPort.findStudentSchedules(99999L, WINDOW_FROM, WINDOW_TO)).isEmpty();
        assertThat(scheduleQueryPort.findTutorSchedules(99999L, WINDOW_FROM, WINDOW_TO)).isEmpty();
    }

    private Lecture lecture(String title, LocalDateTime startTime) {
        return new Lecture(title, startTime, startTime.plusHours(2), "메모", TUTOR_ID, 20);
    }

    private SpecialLecture specialLecture(String title, LocalDateTime startTime) {
        return new SpecialLecture(title, startTime, startTime.plusHours(2), "메모", TUTOR_ID, 30);
    }
}
