package com.example.simplescheduleapp.schedule.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.schedule.application.ScheduleQueryService;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;
import com.example.simplescheduleapp.schedule.presentation.response.ScheduleGetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정(캘린더) API.
 *
 * <p><b>경로에 식별자가 없다.</b> 조회 대상도 역할도 토큰에서만 온다 — 남의 식별자를 넣어
 * 남의 일정을 보는 경로를 애초에 만들지 않는다(ADR-0005).
 *
 * <p>역할 제한({@code @RequireRole})은 두지 않는다. 세 역할 모두 자기 일정을 볼 수 있어야 하고,
 * "무엇이 내 일정인가"는 서비스가 역할에 따라 가른다.
 */
@RequiredArgsConstructor
@RestController
public class ScheduleController {

    private final ScheduleQueryService scheduleQueryService;

    /**
     * 내 일정 조회.
     *
     * <p>{@code from}/{@code to}는 <b>필수</b>다. 창이 없으면 전체 이력을 긁는 질의가 되는데,
     * 캘린더는 언제나 보고 있는 구간이 있다. 창에 걸친 일정(구간 시작 전에 시작해 구간 안까지
     * 이어지는 것)도 포함된다 — 그러지 않으면 캘린더 왼쪽 끝에 구멍이 생긴다.
     */
    @GetMapping("/me/schedules")
    public ResponseEntity<ScheduleGetResponse> getMySchedules(
            @Auth Long memberId,
            @Auth Role role,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<ScheduleView> schedules = scheduleQueryService.findMySchedules(memberId, role, from, to);
        return ResponseEntity.ok(ScheduleGetResponse.from(schedules));
    }
}
