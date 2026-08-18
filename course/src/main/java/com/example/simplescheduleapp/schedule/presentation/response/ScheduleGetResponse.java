package com.example.simplescheduleapp.schedule.presentation.response;

import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 일정 응답.
 *
 * <p>배열을 최상위에 두지 않고 객체로 감싼다 — 강의 목록 응답들이 이미 그 모양이라 클라이언트가
 * 목록 응답을 한 가지 방식으로만 다루게 된다.
 */
public record ScheduleGetResponse(
        List<ScheduleItem> scheduleResponses
) {

    public static ScheduleGetResponse from(List<ScheduleView> views) {
        List<ScheduleItem> items = views.stream()
                .map(it -> new ScheduleItem(
                        it.scheduleId(),
                        it.type().name(),
                        it.title(),
                        it.startTime(),
                        it.endTime(),
                        it.memo()))
                .toList();
        return new ScheduleGetResponse(items);
    }

    /**
     * 캘린더 한 칸.
     *
     * <p>{@code type}은 {@code LECTURE | SPECIAL_LECTURE | CONSULTATION}이다. 클라이언트는
     * 이 값으로 항목의 후속 이동을 정한다 — 종류가 없으면 목록에서 아무 데도 갈 수 없다.
     * 문자열로 내보내는 것은 새 종류가 추가돼도 구 클라이언트가 파싱에 실패하지 않게 하기
     * 위해서다(모르는 값은 무시하면 된다).
     */
    public record ScheduleItem(
            Long scheduleId,
            String type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String memo
    ) {
    }
}
