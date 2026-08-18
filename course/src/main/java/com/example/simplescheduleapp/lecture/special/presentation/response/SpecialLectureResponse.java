package com.example.simplescheduleapp.lecture.special.presentation.response;

import java.time.LocalDateTime;

/**
 * 특강 목록 응답 항목.
 *
 * <p>필드 이름은 {@link SpecialLectureCreateResponse}와 맞춘다 — 같은 것을 두 경로에서 받는데
 * 이름이 다르면 클라이언트가 타입을 두 번 정의하게 된다.
 *
 * <p><b>{@code enrolledCount}가 없는 이유.</b> 일반 강의의 {@code LectureResponse}와 달리 특강의
 * 잔여 정원은 <b>Redis 카운터가 원천</b>이다(4단계 방어의 1차선). DB의 값은 확정된 수강만
 * 반영하므로 신청이 몰리는 순간 실제와 벌어진다. 목록에 그 값을 실으면 클라이언트가 그것을
 * 실시간 잔여로 오해하므로, 정확한 잔여가 필요한 화면은 상세/신청 경로를 쓰게 둔다.
 */
public record SpecialLectureResponse(
        Long specialLectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {
}
