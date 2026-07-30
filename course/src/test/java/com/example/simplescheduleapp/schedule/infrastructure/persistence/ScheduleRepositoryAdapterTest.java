package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ScheduleRepositoryAdapter}가 네이티브 쿼리로 위임하는 겹침 조회의 회귀 테스트.
 *
 * <p>과거 {@code ScheduleJpaRepository}는 excludeScheduleId가 null인 경우를 구분하기 위해
 * {@code CAST(:excludeScheduleId AS BIGINT) IS NULL} 형태를 썼는데, BIGINT는 MySQL에서 유효한
 * CAST 대상 타입이 아니라 실제 운영 흐름(POST /enrollments/accept)에서 500 에러를 유발했다
 * (H2/PostgreSQL은 CAST(... AS BIGINT)를 허용해 테스트에서 발견되지 않았던 결함).
 *
 * <p>이 테스트는 H2 기준으로 어댑터가 null -> sentinel(-1) 변환을 거쳐도 제외 여부에 따른
 * 결과가 여전히 올바른지 검증한다. MySQL의 CAST 문법 오류 자체는 H2로 재현할 수 없어(H2는
 * BIGINT CAST를 지원) docker exec 기반 수동 검증으로 별도 확인했다.
 */
@Transactional
class ScheduleRepositoryAdapterTest extends ApplicationTest {

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

    @Test
    void 튜터_겹침_조회는_excludeScheduleId가_null이면_모두_포함하고_값이_있으면_해당_스케줄을_제외한다() {
        // given
        Tutor tutor = tutorRepository.save(new Tutor("tutor1", "Password123!", "튜터1", 30, "01011112222", 5));
        Lecture lecture1 = lectureRepository.save(new Lecture(
                "강의1", LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 12, 0), null, tutor, 10));
        Lecture lecture2 = lectureRepository.save(new Lecture(
                "강의2", LocalDateTime.of(2026, 8, 1, 11, 0), LocalDateTime.of(2026, 8, 1, 13, 0), null, tutor, 10));

        LocalDateTime windowStart = LocalDateTime.of(2026, 8, 1, 10, 30);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 1, 11, 30);

        // when: excludeScheduleId == null -> 아무 스케줄도 제외하지 않는다
        List<Long> withoutExclusion = scheduleRepository.findOverlappingScheduleIdsByTutorId(
                tutor.getId(), windowStart, windowEnd, null);

        // then
        assertThat(withoutExclusion).containsExactlyInAnyOrder(lecture1.getId(), lecture2.getId());

        // when: excludeScheduleId == lecture1.id -> lecture1은 결과에서 빠진다
        List<Long> withExclusion = scheduleRepository.findOverlappingScheduleIdsByTutorId(
                tutor.getId(), windowStart, windowEnd, lecture1.getId());

        // then
        assertThat(withExclusion).containsExactly(lecture2.getId());
    }

    @Test
    void 학생_겹침_조회는_excludeScheduleId가_null이면_모두_포함하고_값이_있으면_해당_스케줄을_제외한다() {
        // given
        Tutor tutor = tutorRepository.save(new Tutor("tutor2", "Password123!", "튜터2", 30, "01033334444", 5));
        Student student = studentRepository.save(new Student("student1", "Password123!", "학생1", 20, "01055556666", "테스트고"));

        Lecture lecture1 = lectureRepository.save(new Lecture(
                "강의A", LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 12, 0), null, tutor, 10));
        Lecture lecture2 = lectureRepository.save(new Lecture(
                "강의B", LocalDateTime.of(2026, 8, 1, 11, 0), LocalDateTime.of(2026, 8, 1, 13, 0), null, tutor, 10));

        lectureEnrollmentRepository.save(new LectureEnrollment(lecture1, student));
        lectureEnrollmentRepository.save(new LectureEnrollment(lecture2, student));

        LocalDateTime windowStart = LocalDateTime.of(2026, 8, 1, 10, 30);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 1, 11, 30);

        // when: excludeScheduleId == null -> 아무 스케줄도 제외하지 않는다
        List<Long> withoutExclusion = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                student.getId(), windowStart, windowEnd, null);

        // then
        assertThat(withoutExclusion).containsExactlyInAnyOrder(lecture1.getId(), lecture2.getId());

        // when: excludeScheduleId == lecture1.id (수강 신청 수락 시 자기 자신 제외 시나리오) -> lecture1은 빠진다
        List<Long> withExclusion = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                student.getId(), windowStart, windowEnd, lecture1.getId());

        // then
        assertThat(withExclusion).containsExactly(lecture2.getId());
    }
}
