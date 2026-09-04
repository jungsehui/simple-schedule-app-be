package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.schedule.domain.service.ScheduleConflictValidator;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentService {

    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;
    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;
    private final ScheduleConflictValidator scheduleConflictValidator;

    @Transactional
    public SpecialLectureEnrollment enrollSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
        Student student = studentRepository.getById(studentId);
        scheduleConflictValidator.validateNoStudentConflict(student.getId(), specialLecture.getStartTime(), specialLecture.getEndTime(), null);
        SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student.getId());
        specialLectureRepository.save(specialLecture);
        return specialLectureEnrollmentRepository.save(specialLectureEnrollment);
    }

    /**
     * 신청을 지운다. <b>삭제된 행 수를 그대로 돌려준다.</b>
     *
     * <p>좌석 반환 판단을 여기서 하지 않는 이유: Redis 증가는 <b>커밋 이후</b>여야 한다.
     * 트랜잭션 안에서 좌석을 돌려주면 이후 롤백 시 <b>존재하는 신청의 좌석이 풀려 초과 판매</b>가
     * 된다. 그래서 이 메서드는 DB 사실만 확정하고, 좌석 반환은 호출자가 커밋 후에 한다.
     */
    @Transactional
    public int cancelSpecialLectureEnrollment(Long specialLectureId, Long studentId) {
        return specialLectureEnrollmentRepository.deleteBySpecialLectureIdAndStudentId(specialLectureId, studentId);
    }
}
