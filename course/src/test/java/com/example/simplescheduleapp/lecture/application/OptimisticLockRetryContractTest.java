package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.LectureService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 낙관적 락 재시도 계약 테스트 — 강의 일반 경로의 쓰기 메서드가
 * OptimisticLockingFailureException 재시도(@Retryable)를 유지하는지 고정한다.
 * (stage/1-3 통합분: Schedule.version 충돌 시 3회 재시도, 100ms 백오프)
 */
class OptimisticLockRetryContractTest {

    @Test
    void 강의_쓰기_경로에_낙관적_락_재시도가_설정되어_있다() {
        List<Method> retryables = Arrays.stream(new Class<?>[]{LectureService.class, LectureEnrollmentService.class})
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(Retryable.class))
                .toList();

        assertThat(retryables).hasSizeGreaterThanOrEqualTo(3);

        for (Method m : retryables) {
            Retryable r = m.getAnnotation(Retryable.class);
            assertThat(r.retryFor()).contains(OptimisticLockingFailureException.class);
            assertThat(r.maxAttempts()).isEqualTo(3);
        }
    }
}
