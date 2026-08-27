package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.student.domain.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Member 생성 시 username 은(는)")
class MemberUsernameTest {

    @DisplayName("신규 가입에서 소문자로 정규화되어 저장된다")
    @Test
    void 신규_가입은_정규화된다() {
        Student student = new Student("Abc12", "Password1!", "정세희", 20, "01012341234", "학교");

        assertThat(student.getUsername()).isEqualTo("abc12");
    }

    @DisplayName("신규 가입에서 규칙 위반은 거부된다")
    @Test
    void 신규_가입의_규칙_위반은_거부된다() {
        assertThatThrownBy(() ->
                new Student("ab", "Password1!", "정세희", 20, "01012341234", "학교"))
                .isInstanceOf(ApplicationException.class);
    }

    /**
     * DB 복원 경로는 재검증하지 않는다. 규칙이 바뀌기 전에 저장된 값이 있어도
     * 로드가 실패하면 안 된다 — Schedule.reconstitute가 validatePastTime을 건너뛰는 것과 같다.
     *
     * <p>{@code Student}에는 {@code reconstitute} 정적 팩토리가 없다(Lecture/Consultation과
     * 달리 이 도메인은 DB 복원용 생성자를 직접 노출한다) — 그래서 브리프의
     * {@code Student.reconstitute(...)} 대신 실제 시그니처인
     * {@code new Student(Long, String, Password, ...)}를 쓴다. {@code Password.of(...)}도
     * 존재하지 않아 실제 public 생성자 {@code new Password(String)}으로 대체한다.
     */
    @DisplayName("DB 복원 경로는 옛 규칙 값도 그대로 받는다")
    @Test
    void DB_복원은_재검증하지_않는다() {
        Student restored = new Student(
                1L, "OldStyleName", new Password("$2a$12$dummy"),
                "정세희", 20, "01012341234", "학교");

        assertThat(restored.getUsername()).isEqualTo("OldStyleName");
    }
}
