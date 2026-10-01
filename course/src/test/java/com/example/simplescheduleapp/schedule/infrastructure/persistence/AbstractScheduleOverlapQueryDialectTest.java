package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 겹침 조회 네이티브 쿼리({@link ScheduleJpaRepository})를 실제 DB 엔진에서 검증하는 방언 회귀 테스트.
 *
 * <p>{@link ScheduleRepositoryAdapterTest}는 H2에서 돌기 때문에, H2는 허용하지만 실제 엔진은 거부하는
 * 문법을 잡지 못한다. 실제로 {@code CAST(:excludeScheduleId AS BIGINT)}가 H2 테스트를 통과한 채
 * 로컬 MySQL에서 {@code POST /enrollments/accept} 500을 냈다. 하위 클래스가 엔진별 컨테이너를
 * {@code @ServiceConnection}으로 연결하고, 시나리오는 여기 한 곳에서 공유한다.
 *
 * <p>스키마는 Hibernate {@code ddl-auto: create}로 만든다. prod의 Flyway 마이그레이션({@code :app}의
 * {@code V1__baseline.sql})과 같은 스키마라는 보장은 없다.
 */
@Transactional
@SpringBootTest
abstract class AbstractScheduleOverlapQueryDialectTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 8, 1, 10, 30);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 8, 1, 11, 30);

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Autowired
    private SpecialLectureRepository specialLectureRepository;

    @Autowired
    private SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 튜터_겹침_조회는_제외_대상이_없으면_모두_포함하고_있으면_그_스케줄만_뺀다() {
        // given
        Tutor tutor = tutorRepository.save(new Tutor("dtutor1", "Password123!", "튜터1", 30, "01011112222", 5));
        Lecture lecture = lectureRepository.save(lecture("강의1", 10, 12, tutor.getId()));
        SpecialLecture specialLecture = specialLectureRepository.save(specialLecture("특강1", 11, 13, tutor.getId()));

        // when
        List<Long> withoutExclusion = scheduleRepository.findOverlappingScheduleIdsByTutorId(
                tutor.getId(), WINDOW_START, WINDOW_END, null);
        List<Long> withExclusion = scheduleRepository.findOverlappingScheduleIdsByTutorId(
                tutor.getId(), WINDOW_START, WINDOW_END, lecture.getId());

        // then
        assertThat(withoutExclusion).containsExactlyInAnyOrder(lecture.getId(), specialLecture.getId());
        assertThat(withExclusion).containsExactly(specialLecture.getId());
    }

    @Test
    void 학생_겹침_조회는_일반강의와_특강_수강을_합치고_제외_대상이_있으면_그_스케줄만_뺀다() {
        // given: UNION 양쪽(lecture_enrollment, special_lecture_enrollment)을 모두 탄다
        Tutor tutor = tutorRepository.save(new Tutor("dtutor2", "Password123!", "튜터2", 30, "01033334444", 5));
        Student student = studentRepository.save(new Student("dstudent1", "Password123!", "학생1", 20, "01055556666", "테스트고"));
        Lecture lecture = lectureRepository.save(lecture("강의A", 10, 12, tutor.getId()));
        SpecialLecture specialLecture = specialLectureRepository.save(specialLecture("특강A", 11, 13, tutor.getId()));
        lectureEnrollmentRepository.save(new LectureEnrollment(lecture.getId(), student.getId()));
        specialLectureEnrollmentRepository.save(new SpecialLectureEnrollment(specialLecture.getId(), student.getId()));

        // when: 수강 신청 수락 시 자기 자신(lecture)을 제외하는 시나리오 포함
        List<Long> withoutExclusion = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                student.getId(), WINDOW_START, WINDOW_END, null);
        List<Long> withExclusion = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                student.getId(), WINDOW_START, WINDOW_END, lecture.getId());

        // then
        assertThat(withoutExclusion).containsExactlyInAnyOrder(lecture.getId(), specialLecture.getId());
        assertThat(withExclusion).containsExactly(specialLecture.getId());
    }

    @Test
    void 학생_겹침_조회는_소프트삭제된_스케줄과_시간이_맞닿기만_한_스케줄을_제외한다() {
        // given
        Tutor tutor = tutorRepository.save(new Tutor("dtutor3", "Password123!", "튜터3", 30, "01077778888", 5));
        Student student = studentRepository.save(new Student("dstudent2", "Password123!", "학생2", 20, "01099990000", "테스트고"));
        Lecture deleted = lectureRepository.save(lecture("삭제된강의", 10, 12, tutor.getId()));
        Lecture touching = lectureRepository.save(lecture("맞닿는강의", 9, 10, tutor.getId()));
        lectureEnrollmentRepository.save(new LectureEnrollment(deleted.getId(), student.getId()));
        lectureEnrollmentRepository.save(new LectureEnrollment(touching.getId(), student.getId()));
        softDelete(deleted.getId());

        // when: 10:00~11:00 창. touching(09~10)은 끝이 창의 시작과 같아 겹치지 않는다
        List<Long> overlaps = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                student.getId(), LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0), null);

        // then
        assertThat(overlaps).isEmpty();
    }

    private static Lecture lecture(String title, int startHour, int endHour, Long tutorId) {
        return new Lecture(title, at(startHour), at(endHour), null, tutorId, 10);
    }

    private static SpecialLecture specialLecture(String title, int startHour, int endHour, Long tutorId) {
        return new SpecialLecture(title, at(startHour), at(endHour), null, tutorId, 10);
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 8, 1, hour, 0);
    }

    // ScheduleEntity의 @SQLDelete와 같은 UPDATE. 영속성 컨텍스트의 INSERT를 먼저 내보내야 JDBC가 행을 본다.
    private void softDelete(Long scheduleId) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE schedule SET deleted_date = CURRENT_TIMESTAMP WHERE schedule_id = ?", scheduleId);
    }
}
