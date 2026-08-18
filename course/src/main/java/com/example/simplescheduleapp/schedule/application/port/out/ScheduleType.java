package com.example.simplescheduleapp.schedule.application.port.out;

/**
 * 일정의 종류.
 *
 * <p>{@code schedule} 테이블의 JOINED 상속 discriminator({@code type} 컬럼)와 1:1 대응한다.
 * 클라이언트는 이 값으로 캘린더 항목의 후속 동작을 정한다 — 강의는 {@code /lectures/{id}}로,
 * 특강은 특강 상세로 이어진다. 종류를 응답에 싣지 않으면 목록에서 아무 데도 갈 수 없다.
 *
 * <p>영속 계층의 문자열을 그대로 노출하지 않고 여기서 한 번 번역하는 이유는, discriminator가
 * 스키마 사정으로 바뀌어도 API 계약은 그대로 두기 위해서다.
 */
public enum ScheduleType {
    LECTURE,
    SPECIAL_LECTURE,
    CONSULTATION
}
