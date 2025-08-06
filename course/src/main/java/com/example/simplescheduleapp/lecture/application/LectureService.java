package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.event.LectureUpdatedEvent;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final TutorRepository tutorRepository;
    private final ApplicationEventPublisher eventPublisher;

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

    @Transactional
    public Lecture updateLecture(LectureUpdateCommand command) {
        Tutor tutor = tutorRepository.getById(command.tutorId());
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        Schedule schedule = command.toSchedule();
        lecture.update(tutor, schedule, command.capacity());
        eventPublisher.publishEvent(new LectureUpdatedEvent(lecture.getId(), tutor.getId(), command.memo()));
        return lectureRepository.save(lecture);
    }
}
