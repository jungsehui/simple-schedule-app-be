package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마감 검사 Lua 스크립트를 <b>실제 Redis에서</b> 검증한다.
 *
 * <p><b>왜 목으로는 안 되는가.</b> 검사 순서(마감 → 정원)는 Lua 안에 있다. {@code AtomicCounter}를
 * 목으로 대체하면 스크립트가 실행되지 않으므로, 반환값을 스텁해 놓고 그 값을 확인하는 꼴이 된다.
 * 그런 테스트는 순서를 뒤집어도 통과한다 — 아무것도 잡지 못한다.
 *
 * <p>CI는 redis 서비스 컨테이너를 띄우고 테스트 프로파일이 {@code TEST_REDIS_HOST}로 그것을
 * 가리킨다. 로컬은 {@code docker compose up -d}의 6379다.
 */
@DisplayName("마감 검사 Lua 스크립트 은(는)")
class DeadlineAwareDecrementScriptTest extends ApplicationTest {

    @Autowired
    private AtomicCounter atomicCounter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String suffix = UUID.randomUUID().toString();

    private String countKey() {
        return "test:script:" + suffix + ":count";
    }

    private String deadlineKey() {
        return "test:script:" + suffix + ":deadline";
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private void givenCount(long value) {
        atomicCounter.set(countKey(), value, Duration.ofMinutes(5));
    }

    private void givenDeadline(long epochSecond) {
        atomicCounter.set(deadlineKey(), epochSecond, Duration.ofMinutes(5));
    }

    private void cleanUp() {
        redisTemplate.delete(List.of(countKey(), deadlineKey()));
    }

    /**
     * <b>이 테스트가 검사 순서를 고정한다.</b>
     *
     * <p>정원을 먼저 보는 스크립트라면 자리가 없으므로 -1(자리 없음)이 나온다. 그러면
     * 사용자는 "자리가 나면 된다"고 읽고 다시 시도할 이유를 갖는다. 끝난 특강에는 자리가
     * 나지 않으므로 그 재시도는 영원히 실패한다. 마감을 먼저 봐야 -3이 나온다.
     */
    @DisplayName("마감이 지났고 자리도 없으면 '자리 없음'이 아니라 '마감'을 반환한다")
    @Test
    void 마감이_정원보다_먼저_검사된다() {
        givenCount(0);
        givenDeadline(now() - 60);

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(-3L);
        cleanUp();
    }

    @DisplayName("마감이 지났으면 자리가 남아 있어도 감소하지 않는다")
    @Test
    void 마감_후에는_감소하지_않는다() {
        givenCount(10);
        givenDeadline(now() - 60);

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(-3L);
        assertThat(redisTemplate.opsForValue().get(countKey())).isEqualTo("10");
        cleanUp();
    }

    @DisplayName("마감 전이고 자리가 있으면 감소한다")
    @Test
    void 마감_전_정상_신청은_감소한다() {
        givenCount(10);
        givenDeadline(now() + 3600);

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(9L);
        cleanUp();
    }

    @DisplayName("마감 전인데 자리가 없으면 '자리 없음'이다")
    @Test
    void 마감_전_소진은_자리_없음이다() {
        givenCount(0);
        givenDeadline(now() + 3600);

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(-1L);
        cleanUp();
    }

    /**
     * 이 기능 이전에 만들어진 키에는 마감 키가 없다. 그 경우 마감 검사를 건너뛰고 기존 동작을
     * 유지해야 한다 — 그러지 않으면 배포 순간 진행 중인 특강의 신청이 전부 막힌다.
     */
    @DisplayName("마감 키가 없으면 검사를 건너뛰고 기존 동작을 유지한다")
    @Test
    void 마감_키가_없으면_기존_동작이다() {
        givenCount(10);
        // 마감 키를 만들지 않는다

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(9L);
        cleanUp();
    }

    @DisplayName("정원 키가 없으면 '키 없음'이다")
    @Test
    void 정원_키가_없으면_키_없음이다() {
        givenDeadline(now() + 3600);

        Long result = atomicCounter.decrementIfPositiveBefore(countKey(), deadlineKey(), now());

        assertThat(result).isEqualTo(-2L);
        cleanUp();
    }
}
