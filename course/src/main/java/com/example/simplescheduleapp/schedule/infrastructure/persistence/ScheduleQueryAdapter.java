package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.application.port.out.ScheduleQueryPort;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleType;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code ScheduleQueryPort}의 JPA 어댑터.
 *
 * <p>SQL 결과({@link ScheduleRow})를 유스케이스가 보는 모양({@link ScheduleView})으로 번역한다.
 * discriminator 문자열 → {@link ScheduleType} 변환도 여기서 끝난다 — 스키마 사정이 application
 * 위로 새지 않게 하는 것이 이 어댑터의 존재 이유다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ScheduleQueryAdapter implements ScheduleQueryPort {

    private final ScheduleQueryJpaRepository jpaRepository;

    @Override
    public List<ScheduleView> findTutorSchedules(Long tutorId, LocalDateTime from, LocalDateTime to) {
        return toViews(jpaRepository.findTutorSchedules(tutorId, from, to));
    }

    @Override
    public List<ScheduleView> findStudentSchedules(Long studentId, LocalDateTime from, LocalDateTime to) {
        return toViews(jpaRepository.findStudentSchedules(studentId, from, to));
    }

    @Override
    public List<ScheduleView> findParentSchedules(Long parentId, LocalDateTime from, LocalDateTime to) {
        return toViews(jpaRepository.findParentSchedules(parentId, from, to));
    }

    private List<ScheduleView> toViews(List<ScheduleRow> rows) {
        return rows.stream()
                .map(row -> new ScheduleView(
                        row.getScheduleId(),
                        toType(row.getType()),
                        row.getTitle(),
                        row.getStartTime(),
                        row.getEndTime(),
                        row.getMemo()))
                .toList();
    }

    /**
     * discriminator → {@link ScheduleType}.
     *
     * <p>모르는 값이 오면 <b>그 항목만 버리지 않고 예외로 올린다.</b> 조용히 건너뛰면 캘린더에
     * 구멍이 생기고, 사용자는 "일정이 없다"로 읽는다 — 없는 것과 못 읽은 것은 다르다.
     * 새 스케줄 종류를 추가하면서 이 enum을 빠뜨리면 여기서 즉시 드러난다.
     */
    private static ScheduleType toType(String discriminator) {
        try {
            return ScheduleType.valueOf(discriminator);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("알 수 없는 schedule discriminator: {}", discriminator);
            throw new IllegalStateException("지원하지 않는 일정 종류입니다: " + discriminator, e);
        }
    }
}
