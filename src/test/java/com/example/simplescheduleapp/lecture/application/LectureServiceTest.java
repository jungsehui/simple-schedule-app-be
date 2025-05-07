package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
        Lecture createdLecture = lectureRepository.getById(lecture.getId());

        assertThat(createdLecture).isNotNull();
        assertThat(createdLecture.getTitle()).isEqualTo("테스트 제목");
        assertThat(createdLecture.getStartTime()).isEqualTo(now);
        assertThat(createdLecture.getEndTime()).isEqualTo(now.plusHours(1));
        assertThat(createdLecture.getMemo()).isEqualTo("테스트 메모");
        assertThat(createdLecture.getCapacity()).isEqualTo(5);
        assertThat(createdLecture.getEnrolledCount()).isEqualTo(0);
        assertThat(createdLecture.getTutor().getId()).isEqualTo(savedTutor.getId());
    }

    @Test
    void 강의_수정_요청이_들어오면_강의를_수정한다() {
        Tutor savedTutor = tutorRepository.save(tutor);
        Lecture lecture = new Lecture("생성 강의", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "생성 메모", savedTutor, 3);

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
