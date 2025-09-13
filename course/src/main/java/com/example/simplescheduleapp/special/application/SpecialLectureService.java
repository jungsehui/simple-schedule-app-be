package com.example.simplescheduleapp.special.application;

import com.example.simplescheduleapp.special.application.command.SpecialLectureCreateCommand;
import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureService {

    private final String INITIAL_ENROLLED_COUNT_TO_ZERO_STRING = "0";

    private final SpecialLectureRepository specialLectureRepository;
    private final TutorRepository tutorRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public SpecialLecture createSpecialLecture(SpecialLectureCreateCommand command) {
        Tutor tutor = tutorRepository.getById(command.memberId());
        SpecialLecture specialLecture = new SpecialLecture(
                command.title(), command.startTime(), command.endTime(), command.memo(), tutor, command.capacity()
        );

        // 1. 먼저 DB에 저장하여 ID를 부여받습니다.
        SpecialLecture savedSpecialLecture = specialLectureRepository.save(specialLecture);

        // 2. 이제 부여받은 ID를 안전하게 사용할 수 있습니다.
        String lectureId = savedSpecialLecture.getId().toString();
        String countKey = "special_lecture:" + lectureId + ":enrolled_count";
        String capacityKey = "special_lecture:" + lectureId + ":capacity";

        stringRedisTemplate.opsForValue().set(countKey, INITIAL_ENROLLED_COUNT_TO_ZERO_STRING);
        stringRedisTemplate.opsForValue().set(capacityKey, String.valueOf(savedSpecialLecture.getCapacity()));

        return savedSpecialLecture;
    }
}
