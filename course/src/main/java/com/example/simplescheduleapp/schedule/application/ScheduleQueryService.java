package com.example.simplescheduleapp.schedule.application;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleQueryPort;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.schedule.exception.ScheduleExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 캘린더 조회 유스케이스.
 *
 * <p>같은 "내 일정"이라도 역할마다 스키마상 도달 경로가 다르다 — 강사는 자기가 여는 강의·특강·상담,
 * 학생은 자기가 신청한 강의·특강, 학부모는 자기가 참석하는 상담이다. 이 분기가 이 서비스의
 * 유일한 책임이다.
 */
@RequiredArgsConstructor
@Service
public class ScheduleQueryService {

    /**
     * 조회 창 상한.
     *
     * <p>창이 없으면 전체 이력을 한 번에 긁는 질의가 되고, 캘린더 한 화면이 필요로 하는 것보다
     * 훨씬 크다. 월 단위 화면 몇 개를 앞뒤로 넘기는 용도는 충분히 덮는 값으로 잡았다.
     */
    private static final Duration MAX_WINDOW = Duration.ofDays(366);

    private final ScheduleQueryPort scheduleQueryPort;

    public List<ScheduleView> findMySchedules(Long memberId, Role role, LocalDateTime from, LocalDateTime to) {
        validateWindow(from, to);
        return switch (role) {
            case TUTOR -> scheduleQueryPort.findTutorSchedules(memberId, from, to);
            case STUDENT -> scheduleQueryPort.findStudentSchedules(memberId, from, to);
            case PARENT -> scheduleQueryPort.findParentSchedules(memberId, from, to);
        };
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new ApplicationException(ScheduleExceptionCode.INVALID_SCHEDULE_WINDOW);
        }
        if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new ApplicationException(ScheduleExceptionCode.SCHEDULE_WINDOW_TOO_WIDE);
        }
    }
}
