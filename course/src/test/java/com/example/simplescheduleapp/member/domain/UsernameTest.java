package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

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

    /**
     * 정준 생성자로도 규칙을 우회할 수 없다.
     *
     * <p>record는 정준 생성자를 private으로 만들 수 없으므로, 그 경로가 열려 있으면
     * of()를 우회해 규칙 밖 값이 들어온다 — 이 클래스의 존재 이유가 무너지는 자리다.
     */
    @DisplayName("정준 생성자도 규칙을 검사한다 — of()를 우회할 수 없다")
    @Test
    void 정준_생성자도_규칙을_검사한다() {
        assertThatThrownBy(() -> new Username("Abc12"))   // 정규화 안 된 값
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> new Username("ab"))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> new Username(null))
                .isInstanceOf(ApplicationException.class);
    }

    /**
     * 로케일에 따라 소문자화 결과가 달라지면 안 된다.
     *
     * <p>튀르키예어 로케일에서 "ADMIN".toLowerCase()는 "admın"(dotless i)이 되어 규칙을
     * 통과하지 못한다. 이 저장소에는 로케일 고정 설정이 없으므로 JVM 기본 로케일에
     * 의존하면 배포 환경에 따라 동작이 갈린다.
     */
    @DisplayName("로케일과 무관하게 정규화한다")
    @Test
    void 로케일과_무관하게_정규화한다() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(Username.of("ADMIN").value()).isEqualTo("admin");
        } finally {
            Locale.setDefault(original);
        }
    }
}
