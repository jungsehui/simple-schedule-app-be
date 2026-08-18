package com.example.simplescheduleapp.schedule.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 아웃바운드 포트: 회원의 일정 조회 (구현: infrastructure의 JPA 어댑터).
 *
 * <p>쓰기 쪽 {@code ScheduleRepository}(도메인 포트)와 분리한다 — 저쪽은 충돌 검사를 위해
 * ID만 돌려주는 불변식 보조 질의이고, 이쪽은 화면을 채우는 조회다. 반환 타입부터 다르며
 * (애그리게잇 아님, {@link ScheduleView}), 섞으면 도메인 포트가 화면 사정에 끌려다닌다.
 *
 * <p><b>역할별로 질의가 다른 이유.</b> 같은 "내 일정"이라도 스키마상 도달 경로가 다르다.
 * 강사는 강의·특강·상담의 {@code tutor_id}로, 학생은 수강신청 두 테이블로, 학부모는
 * 상담 참석자로 이어진다. 하나의 질의로 합치면 세 경로를 모두 OUTER JOIN해야 해서
 * 읽기도 최적화도 어려워진다.
 */
public interface ScheduleQueryPort {

    List<ScheduleView> findTutorSchedules(Long tutorId, LocalDateTime from, LocalDateTime to);

    List<ScheduleView> findStudentSchedules(Long studentId, LocalDateTime from, LocalDateTime to);

    List<ScheduleView> findParentSchedules(Long parentId, LocalDateTime from, LocalDateTime to);
}
