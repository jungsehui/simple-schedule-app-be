package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentFacade {

    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;
    private final SpecialLectureRepository specialLectureRepository;

    private static final int LOCK_TIMEOUT_SECONDS = 5;

    public Long enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        String lockName = "special_lecture_lock_" + command.specialLectureId();
        try {
            Integer result = specialLectureRepository.getLock(lockName, LOCK_TIMEOUT_SECONDS);
            if (result == null || result != 1) {
                throw new RuntimeException("Failed to acquire lock.");
            }

            SpecialLectureEnrollment enrolled = specialLectureEnrollmentService.enrollSpecialLectureEnrollment(
                    command.specialLectureId(),
                    command.studentId()
            );
            return enrolled.getId();
        } finally {
            specialLectureRepository.releaseLock(lockName);
        }
    }
}
