package com.example.simplescheduleapp.sse.cache;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.sse.exception.SseExceptionCode;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SseEmitterRepositoryTest extends ApplicationTest {

    @Autowired
    private SseEmitterRepository sseEmitterRepository;

    @Test
    void save_후_get_으로_동일한_emitter_반환() {
        // given
        Long memberId = 1L;
        SseEmitter emitter = new SseEmitter();

        // when
        sseEmitterRepository.save(memberId, emitter);

        // then
        SseEmitter result = sseEmitterRepository.get(memberId);
        assertThat(result).isEqualTo(emitter);
    }

    @Test
    void get_호출시_emitter가_없으면_예외발생() {
        // given
        Long memberId = 999L;

        // expect
        assertThatThrownBy(() -> sseEmitterRepository.get(memberId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(SseExceptionCode.SSE_NOT_FOUND.getMessage());
    }

    @Test
    void delete_후_get_호출_시_예외발생() {
        // given
        Long memberId = 2L;
        SseEmitter emitter = new SseEmitter();
        sseEmitterRepository.save(memberId, emitter);

        // when
        sseEmitterRepository.delete(memberId);

        // then
        assertThatThrownBy(() -> sseEmitterRepository.get(memberId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(SseExceptionCode.SSE_NOT_FOUND.getMessage());
    }
}
