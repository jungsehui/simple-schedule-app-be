package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureService {

    private final SpecialLectureRepository specialLectureRepository;
    private final TutorRepository tutorRepository;

    public SpecialLecture createSpecialLecture(SpecialLectureCreateCommand command) {
        Tutor tutor = tutorRepository.getById(command.memberId());
        SpecialLecture specialLecture = new SpecialLecture(
                command.title(), command.startTime(), command.endTime(), command.memo(), tutor, command.capacity()
        );
        return specialLectureRepository.save(specialLecture);
    }
}
