package com.example.simplescheduleapp.lecture.domain.service;

import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureRepository;
import com.example.simplescheduleapp.lecture.event.LectureUpdatedEvent;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class LectureUpdate {

    private final LectureRepository lectureRepository;
    private final TutorRepository tutorRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Lecture update(Long tutorId, Schedule schedule, int capacity) {
        Tutor tutor = tutorRepository.getById(tutorId);
        Lecture lecture = lectureRepository.getByLectureId(schedule.getId());
        lecture.update(tutor, schedule, capacity);

        log.info("Try to update Lecture. tutor ID {}, lecture ID {}", tutor.getId(), lecture.getId());
        Lecture updatedLecture = lectureRepository.save(lecture);
        log.info("Successfully updated Lecture. ID {}", updatedLecture.getId());

        eventPublisher.publishEvent(new LectureUpdatedEvent(updatedLecture.getId(), tutor.getId(), updatedLecture.getMemo()));
        return updatedLecture;
    }
}
