package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * 네이티브 질의 결과 행 — Spring Data 인터페이스 프로젝션.
 *
 * <p>{@code infrastructure}에 두는 이유는 이것이 <b>SQL 결과의 모양</b>이기 때문이다.
 * 유스케이스가 보는 모양은 {@code ScheduleView}이고, 둘 사이 번역은 어댑터가 한다.
 * 하나로 합치면 컬럼 구성이 바뀔 때 application까지 흔들린다.
 */
public interface ScheduleRow {

    Long getScheduleId();

    /** {@code schedule.type} discriminator 원문 — 어댑터가 {@code ScheduleType}으로 번역한다. */
    String getType();

    String getTitle();

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();

    String getMemo();
}
