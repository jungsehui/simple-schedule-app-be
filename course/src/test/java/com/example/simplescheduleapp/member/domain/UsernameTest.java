package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Username 은(는)")
class UsernameTest {

    @DisplayName("대문자 입력을 거부하지 않고 소문자로 정규화한다")
    @Test
    void 대문자는_거부가_아니라_정규화된다() {
        assertThat(Username.of("Abc12").value()).isEqualTo("abc12");
    }

    @DisplayName("이미 소문자면 그대로 둔다")
    @Test
    void 소문자는_그대로다() {
        assertThat(Username.of("abc_12").value()).isEqualTo("abc_12");
    }

    /**
     * 전부 숫자인 username은 memberId와 혼동된다 — URL·로그·지원 문의에서 실제로 헷갈린다.
     * 그래서 영문자를 최소 하나 요구한다(SSA 기존 규칙에서 가져온 안전 속성).
     */
    @DisplayName("영문자가 하나도 없으면 거부한다")
    @Test
    void 전부_숫자는_거부한다() {
        assertThatThrownBy(() -> Username.of("12345"))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("허용되지 않는 문자가 있으면 거부한다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"abc-12", "abc.12", "abc 12", "abc@12", "한글아이디"})
    void 허용되지_않는_문자는_거부한다(String raw) {
        assertThatThrownBy(() -> Username.of(raw))
                .isInstanceOf(ApplicationException.class);
    }

    @DisplayName("길이 경계: 3자는 통과, 2자는 거부, 20자는 통과, 21자는 거부")
    @Test
    void 길이_경계() {
        assertThat(Username.of("ab1").value()).isEqualTo("ab1");
        assertThatThrownBy(() -> Username.of("ab")).isInstanceOf(ApplicationException.class);

        String twenty = "a" + "1".repeat(19);
        assertThat(Username.of(twenty).value()).isEqualTo(twenty);
        assertThatThrownBy(() -> Username.of(twenty + "1")).isInstanceOf(ApplicationException.class);
    }

    /**
     * 정규화가 검증보다 먼저 일어나야 한다. 대문자를 먼저 거부해 버리면
     * "대문자는 정규화한다"는 규칙이 성립하지 않는다.
     */
    @DisplayName("정규화가 검증보다 먼저다 — 대문자만으로 이루어진 값도 통과한다")
    @Test
    void 정규화가_검증보다_먼저다() {
        assertThat(Username.of("ABC12").value()).isEqualTo("abc12");
    }

    @DisplayName("null은 거부한다")
    @Test
    void null은_거부한다() {
        assertThatThrownBy(() -> Username.of(null))
                .isInstanceOf(ApplicationException.class);
    }
}
