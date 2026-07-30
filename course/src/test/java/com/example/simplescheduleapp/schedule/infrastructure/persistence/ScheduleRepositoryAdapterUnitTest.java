package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link ScheduleRepositoryAdapter}가 null -> sentinel(-1) 변환을 실제로 수행하는지 검증하는
 * 단위(Mockito) 회귀 테스트.
 *
 * <p>과거 어댑터는 excludeScheduleId(null 가능)를 그대로 {@code ScheduleJpaRepository}에 넘겼고,
 * 네이티브 쿼리는 {@code CAST(:excludeScheduleId AS BIGINT) IS NULL} 형태로 NULL 여부를 직접
 * 검사했다. BIGINT는 MySQL에서 유효한 CAST 대상 타입이 아니라 실제 운영 흐름
 * (POST /enrollments/accept)에서 500 에러를 유발했다.
 *
 * <p>이 테스트는 "null이 네이티브 쿼리까지 전달되면 안 된다"는 불변식을 고정한다. 예전 어댑터
 * 코드(null을 그대로 전달)로는 실패하고, 현재 코드(sentinel -1L로 치환)로는 통과한다 — SQL 엔진
 * 없이도 회귀를 잡아낼 수 있는 지점이다.
 */
class ScheduleRepositoryAdapterUnitTest extends MockTestSupport {

    @Mock
    private ScheduleJpaRepository jpaRepository;

    @InjectMocks
    private ScheduleRepositoryAdapter adapter;

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Test
    void 튜터_조회에서_excludeScheduleId가_null이면_native_쿼리에는_sentinel이_바인딩된다() {
        // given
        given(jpaRepository.findOverlappingScheduleIdsByTutorId(any(), any(), any(), anyLong()))
                .willReturn(List.of());

        // when: 포트에는 null을 전달한다 (예: 신규 강의 생성 시 제외할 스케줄 없음)
        adapter.findOverlappingScheduleIdsByTutorId(1L, start, end, null);

        // then: JPA 네이티브 쿼리에는 NULL이 아니라 sentinel(-1)이 바인딩되어야 한다
        then(jpaRepository).should()
                .findOverlappingScheduleIdsByTutorId(eq(1L), eq(start), eq(end), eq(-1L));
    }

    @Test
    void 학생_조회에서_excludeScheduleId가_null이면_native_쿼리에는_sentinel이_바인딩된다() {
        // given
        given(jpaRepository.findOverlappingScheduleIdsByStudentId(any(), any(), any(), anyLong()))
                .willReturn(List.of());

        // when: 포트에는 null을 전달한다 (예: 수강 신청 수락 시 자기 자신을 제외하지 않는 경로)
        adapter.findOverlappingScheduleIdsByStudentId(2L, start, end, null);

        // then
        then(jpaRepository).should()
                .findOverlappingScheduleIdsByStudentId(eq(2L), eq(start), eq(end), eq(-1L));
    }

    @Test
    void excludeScheduleId가_있으면_그_값이_그대로_native_쿼리에_바인딩된다() {
        // given
        given(jpaRepository.findOverlappingScheduleIdsByTutorId(any(), any(), any(), anyLong()))
                .willReturn(List.of());

        // when: 강의 수정처럼 특정 스케줄을 제외하고 싶은 경로
        adapter.findOverlappingScheduleIdsByTutorId(1L, start, end, 42L);

        // then
        then(jpaRepository).should()
                .findOverlappingScheduleIdsByTutorId(eq(1L), eq(start), eq(end), eq(42L));
    }
}
