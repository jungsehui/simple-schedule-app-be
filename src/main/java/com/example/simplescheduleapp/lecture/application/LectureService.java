package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.lecture.domain.repository.LectureRepository;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import com.example.simplescheduleapp.tutor.domain.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final TutorRepository tutorRepository;

    public Lecture createLecture(LectureCreateCommand lectureCreateCommand) {
        Tutor tutor = tutorRepository.getById(lectureCreateCommand.memberId());
        Lecture lecture = lectureCreateCommand.toLecture();
        lecture.validatePastTime(lecture.getStartTime(), lecture.getEndTime());
        lecture.composeTutor(tutor);
        return lectureRepository.save(lecture);
    }

    public Lecture getLecture(Long lectureId) {
        return lectureRepository.getById(lectureId);
    }

    public List<Lecture> findLectures(Long tutorId) {
        return lectureRepository.findAllByTutorId(tutorId);
    }

    public List<Lecture> searchLecturesByKeyword(String keyword) {
        return lectureRepository.findByKeyword(keyword);
    }

    public void updateLecture(LectureUpdateCommand lectureUpdateCommand) {
        Lecture lecture = lectureRepository.getById(lectureUpdateCommand.lectureId());
        lecture.update(
                lectureUpdateCommand.title(),
                lectureUpdateCommand.startTime(),
                lectureUpdateCommand.endTime(),
                lectureUpdateCommand.memo()
        );
    }
}
