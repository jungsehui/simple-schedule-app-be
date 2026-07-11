package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureService {

    private final SpecialLectureRepository specialLectureRepository;
    private final TutorRepository tutorRepository;
    private final SpecialLectureRedisClient specialLectureRedisClient;
    private final ScheduleConflictValidator scheduleConflictValidator;

    @Transactional
    public SpecialLecture createSpecialLecture(SpecialLectureCreateCommand command) {
        Tutor tutor = tutorRepository.getById(command.memberId());
        scheduleConflictValidator.validateNoTutorConflict(tutor.getId(), command.startTime(), command.endTime(), null);
        SpecialLecture specialLecture = new SpecialLecture(
                command.title(), command.startTime(), command.endTime(), command.memo(), tutor, command.capacity()
        );

        // 먼저 DB에 저장하여 ID를 부여
        SpecialLecture savedSpecialLecture = specialLectureRepository.save(specialLecture);
        specialLectureRedisClient.initializeSpecialLecture(
                savedSpecialLecture.getId(), savedSpecialLecture.getCapacity(), savedSpecialLecture.getEndTime()
        );
        return savedSpecialLecture;
    }
}
