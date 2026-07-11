package com.example.simplescheduleapp.lecture.special.domain;

import java.util.Optional;

/**
 * 아웃바운드 포트: 특별 강의 수강신청 영속성 (구현: infrastructure의 JPA 어댑터).
 *
 * <p>순수 자바 인터페이스 — Spring Data/JPA가 도메인에 침투하지 않는다. (ADR-0002 Stage 2)
 */
public interface SpecialLectureEnrollmentRepository {

    SpecialLectureEnrollment save(SpecialLectureEnrollment specialLectureEnrollment);

    Optional<SpecialLectureEnrollment> findById(Long id);
}
