package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.domain.service.LectureUpdate;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LectureService {

    private final LectureUpdate lectureUpdate;
    private final LectureRepository lectureRepository;
    private final TutorRepository tutorRepository;

    public Lecture createLecture(LectureCreateCommand command) {
        Tutor tutor = tutorRepository.getById(command.memberId());
        Lecture lecture = new Lecture(command.title(), command.startTime(), command.endTime(), command.memo(), tutor, command.capacity());
        return lectureRepository.save(lecture);
    }

    public Lecture findLecture(Long lectureId) {
        return lectureRepository.getByLectureId(lectureId);
    }

    public List<Lecture> findAllTutorLectures(Long tutorId) {
        return lectureRepository.findAllByTutorId(tutorId);
    }

    public List<Lecture> searchLectures(String keyword) {
        return lectureRepository.findByKeyword(keyword);
    }

    public Lecture updateLecture(LectureUpdateCommand command) {
        Long tutorId = command.tutorId();
        Schedule schedule = command.toSchedule();
        int capacity = command.capacity();
        return lectureUpdate.update(tutorId, schedule, capacity);
    }
}
