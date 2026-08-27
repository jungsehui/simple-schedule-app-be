package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.member.domain.Password;
import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * username 정규화가 <b>가입과 로그인 양쪽</b>에 적용되는지 본다.
 *
 * <p><b>왜 이 테스트가 필요한가.</b> 가입만 정규화하면 사용자가 잠긴다. {@code Abc12}로
 * 가입하면 DB에는 {@code abc12}가 저장되는데, 로그인 조회가 원시 입력을 그대로 쓰면
 * {@code Abc12}로는 그 행을 찾지 못한다. 사용자는 자기가 방금 입력한 아이디로 로그인할 수 없다.
 *
 * <p><b>로컬에서는 재현되지 않는다.</b> MySQL의 기본 콜레이션은 대소문자를 구분하지 않아
 * 로그인이 우연히 성공한다. 운영 PostgreSQL은 {@code =} 비교가 대소문자를 구분하므로
 * 거기서만 터진다. 그래서 이 테스트는 대소문자를 구분하는 엔진에서 돌아야 의미가 있다.
 */
@DisplayName("username 정규화와 로그인 은(는)")
class UsernameNormalizationLoginTest extends ApplicationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudentRepository studentRepository;

    private static final String RAW_PASSWORD = "Password1!";

    private void signUp(String username, String phone) {
        studentService.signUpStudent(
                new StudentSignUpCommand(username, RAW_PASSWORD, "정세희", 20, phone, "학교"));
    }

    @DisplayName("대문자로 가입하면 소문자로 저장된다")
    @Test
    void 대문자_가입은_소문자로_저장된다() {
        signUp("AbcTest1", "01011110001");

        assertThat(memberRepository.findByUsername("abctest1")).isPresent();
        assertThat(memberRepository.findByUsername("AbcTest1")).isEmpty();
    }

    /**
     * 이것이 잠금 시나리오다. 사용자가 가입할 때 친 문자열 그대로 로그인할 수 있어야 한다.
     * 서버가 저장 시점에 값을 바꿨다는 사실을 사용자는 알지 못한다.
     */
    @DisplayName("가입할 때 친 그대로 로그인할 수 있다")
    @Test
    void 가입한_문자열_그대로_로그인된다() {
        signUp("AbcTest2", "01011110002");

        assertThatCode(() -> memberService.login("AbcTest2", RAW_PASSWORD))
                .doesNotThrowAnyException();
    }

    @DisplayName("정규화된 형태로도 로그인할 수 있다")
    @Test
    void 정규화된_형태로도_로그인된다() {
        signUp("AbcTest3", "01011110003");

        assertThatCode(() -> memberService.login("abctest3", RAW_PASSWORD))
                .doesNotThrowAnyException();
    }

    /**
     * 규칙 통일 전에 가입한 사용자는 친 그대로 저장돼 있다. 로그인 조회를 정규화 하나로만
     * 바꾸면 이 사람들이 전부 잠긴다 — 신규 사용자를 구하려다 기존 사용자를 잃는 것이다.
     */
    @DisplayName("옛 규칙으로 저장된 사용자는 저장된 그대로 로그인된다")
    @Test
    void 옛_규칙으로_저장된_사용자도_로그인된다() {
        studentRepository.save(new Student(
                null, "OldStyleName", Password.hashPassword(RAW_PASSWORD),
                "정세희", 20, "01011110004", "학교"));

        assertThatCode(() -> memberService.login("OldStyleName", RAW_PASSWORD))
                .doesNotThrowAnyException();
    }

    /**
     * 두 세대가 공존하는 최악의 경우다. 운영 PostgreSQL의 유니크 제약은 대소문자를
     * 구분하므로 {@code Coex1}(구세대)과 {@code coex1}(신세대)이 <b>둘 다 존재할 수 있다.</b>
     *
     * <p>이때 조회가 "정규화 먼저"였다면 두 사람 모두 신세대 계정으로 들어가 <b>남의 계정에
     * 로그인</b>하게 된다. 정확 일치를 먼저 두는 이유가 이것이다.
     */
    @DisplayName("구세대와 신세대가 공존해도 각자 자기 계정으로 들어간다")
    @Test
    void 공존해도_각자_자기_계정이다() {
        studentRepository.save(new Student(
                null, "Coex1", Password.hashPassword(RAW_PASSWORD),
                "구세대", 20, "01011110005", "학교"));
        signUp("Coex1", "01011110006");

        Long legacyId = memberService.login("Coex1", RAW_PASSWORD).memberId();
        Long modernId = memberService.login("coex1", RAW_PASSWORD).memberId();

        assertThat(legacyId).isNotEqualTo(modernId);
        assertThat(memberRepository.getById(legacyId).getUsername()).isEqualTo("Coex1");
        assertThat(memberRepository.getById(modernId).getUsername()).isEqualTo("coex1");
    }
}
