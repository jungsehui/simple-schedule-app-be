package com.example.simplescheduleapp.lecture.general.application;

import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;
import com.example.simplescheduleapp.lecture.general.application.command.LectureUpdateCommand;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import com.example.simplescheduleapp.event.LectureUpdatedEvent;
import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final TutorRepository tutorRepository;
    private final ScheduleConflictValidator scheduleConflictValidator;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Lecture createLecture(LectureCreateCommand command) {
        Tutor tutor = tutorRepository.getById(command.memberId());
        scheduleConflictValidator.validateNoTutorConflict(tutor.getId(), command.startTime(), command.endTime(), null);
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

    // maxRetries=2 → 초기 1회 + 재시도 2회 = 총 3회 (구 spring-retry maxAttempts=3과 동일 동작)
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 2, delay = 100)
    @Transactional
    public Lecture updateLecture(LectureUpdateCommand command) {
        Tutor tutor = tutorRepository.getById(command.tutorId());
        Lecture lecture = lectureRepository.getByLectureId(command.lectureId());
        scheduleConflictValidator.validateNoTutorConflict(tutor.getId(), command.startTime(), command.endTime(), lecture.getId());
        Schedule schedule = command.toSchedule();
        int capacity = command.capacity();
        lecture.update(tutor, schedule, capacity);

        log.info("Try to update Lecture. tutor ID {}, lecture ID {}", tutor.getId(), lecture.getId());
        Lecture updatedLecture = lectureRepository.save(lecture);
        log.info("Successfully updated Lecture. ID {}", updatedLecture.getId());

        eventPublisher.publishEvent(new LectureUpdatedEvent(updatedLecture, updatedLecture.getMemo()));
        return updatedLecture;
    }
}
