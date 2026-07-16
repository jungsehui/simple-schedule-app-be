package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.general.application.LectureService;
import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.general.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 각 테스트를 트랜잭션으로 격리해 롤백(ApplicationTest는 @Transactional이 없고 ddl-auto:create라
// 테스트 간 member.username 유니크 제약이 남아 두 번째 테스트가 실패하던 문제 방지)
@Transactional
class LectureServiceTest extends ApplicationTest {

    @Autowired
    LectureService lectureService;

    @Autowired
    LectureRepository lectureRepository;

    @Autowired
    TutorRepository tutorRepository;

    Tutor tutor = new Tutor("jungsehui", "Password123!", "정세희", 25, "01023420594", 3);

    @Test
    void 강의_생성_요청이_들어오면_강의를_저장한다() {
        // given
        Tutor savedTutor = tutorRepository.save(tutor);
        LocalDateTime now = LocalDateTime.now();

        LectureCreateCommand command = new LectureCreateCommand(
                savedTutor.getId(),
                "테스트 제목",
                now,
                now.plusHours(1),
                "테스트 메모",
                5
        );

        // when
        Lecture lecture = lectureService.createLecture(command);

        // then
        Lecture createdLecture = lectureRepository.getByLectureId(lecture.getId());

        assertThat(createdLecture).isNotNull();
        assertThat(createdLecture.getTitle()).isEqualTo("테스트 제목");
        assertThat(createdLecture.getStartTime()).isEqualTo(now);
        assertThat(createdLecture.getEndTime()).isEqualTo(now.plusHours(1));
        assertThat(createdLecture.getMemo()).isEqualTo("테스트 메모");
        assertThat(createdLecture.getCapacity()).isEqualTo(5);
        assertThat(createdLecture.getEnrolledCount()).isEqualTo(0);
        assertThat(createdLecture.getTutorId()).isEqualTo(savedTutor.getId());
    }

    @Test
    void 강의_수정_요청이_들어오면_강의를_수정한다() {
        Tutor savedTutor = tutorRepository.save(tutor);
        Lecture lecture = new Lecture("생성 강의", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "생성 메모", savedTutor.getId(), 3);

        Lecture savedLecture = lectureRepository.save(lecture);
        LocalDateTime now = LocalDateTime.now();

        LectureUpdateCommand command = new LectureUpdateCommand(
                savedTutor.getId(),
                savedLecture.getId(),
                "수정 제목",
                now,
                now.plusHours(1),
                "수정 메모",
                7
        );

        Lecture updatedLecture = lectureService.updateLecture(command);

        assertThat(updatedLecture).isNotNull();
        assertThat(updatedLecture.getTitle()).isEqualTo("수정 제목");
        assertThat(updatedLecture.getStartTime()).isEqualTo(now);
        assertThat(updatedLecture.getEndTime()).isEqualTo(now.plusHours(1));
        assertThat(updatedLecture.getMemo()).isEqualTo("수정 메모");
        assertThat(updatedLecture.getCapacity()).isEqualTo(7);
    }
}
