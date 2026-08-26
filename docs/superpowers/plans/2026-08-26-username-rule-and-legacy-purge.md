# username 규칙 통일과 레거시 해시 소거 — 실행 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통합 username 규칙을 도메인 불변식으로 세우고, 레거시(무염 SHA-256) 해시를 0으로 만들어 `{sha256}` 매처 제거의 길을 연다.

**Architecture:** username 규칙을 `Member` 도메인 값 객체에 두고(현재는 요청 DTO에만 있다) 저장 전 소문자 정규화를 강제한다. 레거시 해시는 **계정을 지우지 않고 비밀번호 재설정으로** 소거한다 — 삭제하면 강사 전원이 레거시라 강의·수강신청까지 연쇄로 사라진다.

**Tech Stack:** Spring Boot 4.1 / Java 21 / Gradle 멀티모듈 / Flyway(운영 전용) / PostgreSQL(운영)·MySQL(로컬)·H2(테스트)

## 이 계획의 범위와 범위 밖

ADR-0003 Stage 5는 독립 배포 가능한 세 덩어리다. **이 계획은 ① 하나만 다룬다.**

| | 내용 | 이 계획 |
|---|---|---|
| ① | username 규칙 통일 + 레거시 해시 소거 | **여기** |
| ② | `account` 테이블 신설·백필·듀얼라이트 | 별도 계획 |
| ③ | JWT 통일 + `T9` + Spring Security 전환 | 별도 계획 |

①을 먼저 떼는 이유: **①이 유일하게 클라이언트 계약을 건드린다.** 큰 인증 변경과 묶으면 클라이언트 릴리스가 그것에 인질이 된다. 그리고 ① 이후에 `account`를 백필하면 깨끗한 데이터에서 시작한다.

## Global Constraints

- **통합 username 규칙(오너 승인): `^(?=.*[a-z])[a-z0-9_]{3,20}$`** — 소문자·숫자·밑줄만, 3~20자, 영문자 최소 1자
- **입력은 거부가 아니라 정규화한다** — 대문자 입력은 소문자로 변환해 저장한다
- **레거시 판별 술어: `password NOT LIKE '$2%'`** — bcrypt가 아닌 것이 레거시다
- **레거시 처리는 폐기가 아니라 비밀번호 재설정이다**(오너 결정 2026-08-26) — 강사 전원이 레거시라 삭제 시 강의가 연쇄로 사라진다
- 빌드 검증은 `./gradlew :common:build :course:build :notification:build :app:build`
- **Flyway는 운영에서만 활성**이다. 로컬(MySQL)·테스트(H2)는 `ddl-auto`다 — 마이그레이션은 테스트로 검증할 수 없고, **Flyway API로 직접 돌리는 테스트**를 써야 한다(V2·V3가 그렇게 검증됐다)
- 각 Task는 독립 revert 가능해야 하고 Task 경계에서 빌드가 green이어야 한다
- 작업 디렉터리는 `.wt-ssa-analysis` worktree다. 사용자 기본 체크아웃 `ssa/`는 origin보다 97커밋 뒤처져 있다

## File Structure

| 파일 | 책임 |
|---|---|
| `course/.../member/domain/Username.java` (신규) | username 규칙과 정규화를 담는 값 객체. 규칙의 **단일 원천** |
| `course/.../member/domain/Member.java` (수정) | 생성자에서 `Username`을 통해 검증·정규화 |
| `course/.../{student,tutor,parent}/presentation/request/*SignUpRequest.java` (수정) | `@Pattern` 정규식을 새 규칙으로. 도메인 규칙의 **미러**이지 원천이 아니다 |

---

## Task 1: username 규칙을 도메인 불변식으로 세운다

**왜 먼저인가:** 지금 규칙이 요청 DTO의 `@Pattern`에만 있다. 도메인이 자기 불변식을 모르므로 DTO를 거치지 않는 경로(테스트, 내부 호출, 향후 `account` 백필)가 규칙을 우회한다. 정규화를 DTO에 두면 같은 문제가 생긴다.

**Files:**
- Create: `course/src/main/java/com/example/simplescheduleapp/member/domain/Username.java`
- Create: `course/src/test/java/com/example/simplescheduleapp/member/domain/UsernameTest.java`

**Interfaces:**
- Produces: `Username.of(String raw)` → `Username` (검증·정규화), `Username.value()` → `String`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`course/src/test/java/com/example/simplescheduleapp/member/domain/UsernameTest.java`

```java
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
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :course:test --tests '*UsernameTest*'`
Expected: 컴파일 실패 — `Username` 클래스가 없다

- [ ] **Step 3: 예외 코드를 추가한다**

`course/src/main/java/com/example/simplescheduleapp/member/exception/MemberExceptionCode.java`에 상수를 추가한다. **기존 코드값과 겹치지 않는 다음 번호를 쓴다** — 파일을 열어 현재 최대값을 확인하고 그다음을 쓸 것.

```java
    INVALID_USERNAME_FORMAT(ErrorKind.BAD_REQUEST, "<다음 번호>", "아이디는 3~20자의 영소문자·숫자·밑줄이어야 하며 영문자를 최소 하나 포함해야 합니다."),
```

- [ ] **Step 4: 최소 구현**

`course/src/main/java/com/example/simplescheduleapp/member/domain/Username.java`

```java
package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 로그인 식별자. <b>규칙의 단일 원천이다</b> — 요청 DTO의 {@code @Pattern}은 이것의 미러다.
 *
 * <p><b>왜 값 객체인가.</b> 규칙이 요청 DTO에만 있으면 DTO를 거치지 않는 경로(내부 호출,
 * 테스트, 향후 account 백필)가 규칙을 우회한다. 정규화도 마찬가지다 — 한 군데서만 하면
 * 다른 경로로 들어온 대문자가 그대로 저장돼 {@code Admin}과 {@code admin}이 공존한다.
 *
 * <p><b>규칙 도출(2026-08-26 통합 결정).</b> 두 시스템의 정규식 중 하나를 고른 것이 아니라
 * 각각의 안전 속성을 취했다:
 * <ul>
 *   <li><b>소문자만</b>(geekchat) — 대소문자 변형은 {@code Admin}/{@code admin} 사칭만 만들고 이득이 없다</li>
 *   <li><b>영문자 1자 이상</b>(SSA) — 전부 숫자면 {@code memberId}와 혼동된다</li>
 *   <li><b>밑줄 허용·최소 3자</b>(geekchat) — 넓은 쪽. 4자를 요구할 안전 근거가 없다</li>
 * </ul>
 *
 * <p>좁히는 변경은 파괴적이고 넓히는 변경은 추가형이라, 좁은 쪽에서 시작한다.
 */
public record Username(String value) {

    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-z])[a-z0-9_]{3,20}$");

    /**
     * 검증하고 정규화한다. <b>정규화가 검증보다 먼저다</b> — 대문자를 먼저 거부하면
     * "대문자 입력은 소문자로 받아 준다"는 규칙이 성립하지 않는다.
     */
    public static Username of(String raw) {
        if (raw == null) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_FORMAT);
        }
        String normalized = raw.toLowerCase();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_FORMAT);
        }
        return new Username(normalized);
    }

    public Username {
        Objects.requireNonNull(value);
    }
}
```

- [ ] **Step 5: 통과를 확인한다**

Run: `./gradlew :course:test --tests '*UsernameTest*'`
Expected: PASS (7 케이스)

- [ ] **Step 6: 반증한다**

`Username.of`에서 `raw.toLowerCase()`를 `raw`로 바꿔 `정규화가_검증보다_먼저다`와 `대문자는_거부가_아니라_정규화된다`가 **빨간불이 되는지** 확인하고 원복한다. 원복 후 `git status`가 깨끗한지 본다.

- [ ] **Step 7: 커밋**

```bash
git add course/src/main/java/com/example/simplescheduleapp/member/domain/Username.java \
        course/src/main/java/com/example/simplescheduleapp/member/exception/MemberExceptionCode.java \
        course/src/test/java/com/example/simplescheduleapp/member/domain/UsernameTest.java
git commit -m "feat(member): username 규칙을 도메인 값 객체로 (통합 규칙 + 소문자 정규화)"
```

---

## Task 2: `Member`가 `Username`을 통과하게 한다

**Files:**
- Modify: `course/src/main/java/com/example/simplescheduleapp/member/domain/Member.java`
- Test: `course/src/test/java/com/example/simplescheduleapp/member/domain/MemberUsernameTest.java` (신규)

**Interfaces:**
- Consumes: `Username.of(String)` (Task 1)
- Produces: 변화 없음 — `Member.getUsername()`은 여전히 `String`을 반환한다

**설계 판단:** `Member.username` 필드 타입을 `Username`으로 바꾸지 **않는다.** 매퍼·응답 DTO·리포지토리가 전부 `String`을 쓰고 있어 타입 교체는 이 Task의 범위를 훨씬 넘는다. 신규 가입 생성자에서 `Username.of()`를 **통과시키고 정규화된 문자열을 저장**하면 불변식은 성립한다.

**DB 복원 생성자에는 적용하지 않는다.** 이미 저장된 값은 재검증하지 않는다 — 과거 데이터 로드가 실패하면 안 된다(`Schedule`의 `reconstitute`가 같은 이유로 `validatePastTime`을 건너뛴다).

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`course/src/test/java/com/example/simplescheduleapp/member/domain/MemberUsernameTest.java`

```java
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
     */
    @DisplayName("DB 복원 경로는 옛 규칙 값도 그대로 받는다")
    @Test
    void DB_복원은_재검증하지_않는다() {
        Student restored = Student.reconstitute(
                1L, "OldStyleName", com.example.simplescheduleapp.member.domain.Password.of("$2a$12$dummy"),
                "정세희", 20, "01012341234", "학교");

        assertThat(restored.getUsername()).isEqualTo("OldStyleName");
    }
}
```

> **주의:** 위 마지막 테스트의 `Student.reconstitute` 시그니처와 `Password.of`는 **실제 코드를 열어 확인하고 맞춰라.** 현재 `Student`에는 `(Long id, String username, Password password, …)` 생성자가 있다. 정적 팩토리가 없으면 그 생성자를 쓰되, `protected`라 테스트가 같은 패키지에 있어야 할 수 있다 — 그 경우 테스트를 `student.domain` 패키지에 두고 파일 경로를 맞춰라.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :course:test --tests '*MemberUsernameTest*'`
Expected: `신규_가입은_정규화된다`가 `"Abc12"`를 받아 실패

- [ ] **Step 3: 최소 구현**

`Member.java`의 **신규 가입용 생성자만** 수정한다.

```java
    /** 신규 가입용 — 평문 비밀번호를 해싱해 보관한다. username은 검증·정규화된다. */
    protected Member(String username, String password, String name, int age, String phoneNumber) {
        this(null, Username.of(username).value(), Password.hashPassword(password), name, age, phoneNumber);
    }
```

DB 복원용 생성자는 **건드리지 않는다.**

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :course:test --tests '*MemberUsernameTest*' --tests '*UsernameTest*'`
Expected: PASS

- [ ] **Step 5: 회귀 확인 — 기존 테스트가 옛 username을 쓰고 있다**

Run: `./gradlew :course:test`
Expected: **일부 실패한다.** 기존 테스트 픽스처가 `jungsehui` 같은 값을 쓰는데 새 규칙에 맞으면 통과하고, 대문자·4자 미만이면 실패한다.

실패하는 픽스처를 **새 규칙에 맞는 값으로 고친다.** 검증 의도를 바꾸지 말고 값만 바꿔라 — 이건 협력자 API 변경에 따른 적응이다.

- [ ] **Step 6: 4모듈 빌드**

Run: `./gradlew :common:build :course:build :notification:build :app:build`
Expected: BUILD SUCCESSFUL

> Docker 데몬이 꺼져 있으면 `@SpringBootTest`가 Redis 연결 실패로 깨진다. 그건 이 변경과 무관하다 — `git stash`로 깨끗한 상태에서 같은 테스트가 동일하게 실패하는지 확인해 무관함을 증명하고 보고하라. 확인이 필요하면 `docker compose up -d`.

- [ ] **Step 7: 커밋**

```bash
git add course/src
git commit -m "feat(member): 신규 가입 경로에 username 검증·정규화 적용"
```

---

## Task 3: 요청 DTO 3종의 정규식을 새 규칙으로

**Files:**
- Modify: `course/src/main/java/com/example/simplescheduleapp/student/presentation/request/StudentSignUpRequest.java`
- Modify: `course/src/main/java/com/example/simplescheduleapp/tutor/presentation/request/TutorSignUpRequest.java`
- Modify: `course/src/main/java/com/example/simplescheduleapp/parent/presentation/request/ParentSignUpRequest.java`
- Test: `course/src/test/java/com/example/simplescheduleapp/member/presentation/SignUpUsernameContractTest.java` (신규)

**왜 DTO도 바꾸는가:** 도메인이 원천이지만 DTO의 `@Pattern`을 그대로 두면 **옛 규칙으로 먼저 거부**되어 도메인까지 도달하지 않는다. 대문자 입력이 `400 ISE3`로 막혀 정규화가 영영 안 일어난다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`course/src/test/java/com/example/simplescheduleapp/member/presentation/SignUpUsernameContractTest.java`

```java
package com.example.simplescheduleapp.member.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.presentation.StudentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 가입 요청 DTO의 {@code @Pattern}이 도메인 규칙의 <b>미러</b>인지 확인한다.
 *
 * <p>미러가 어긋나면 대문자 입력이 DTO 단계에서 400으로 막혀 도메인의 정규화까지 도달하지
 * 못한다 — "대문자는 거부가 아니라 정규화" 규칙이 표면에서 무력화된다.
 *
 * <p>DTO 정규식이 도메인과 글자 그대로 같지 않은 것은 의도된 것이다. 도메인은 <b>정규화 후</b>
 * 값을 보므로 소문자만 받고, DTO는 <b>정규화 전</b> 입력을 보므로 대문자를 허용해야 한다.
 */
@DisplayName("가입 요청의 username 검증 은(는)")
@WebMvcTest(StudentController.class)
class SignUpUsernameContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    private String signUpBody(String username) {
        return """
                { "username": "%s", "password": "Password1!", "name": "정세희",
                  "age": 20, "phoneNumber": "01012341234", "school": "학교" }
                """.formatted(username);
    }

    @DisplayName("대문자 username을 DTO 단계에서 막지 않는다 — 정규화는 도메인의 몫이다")
    @Test
    void 대문자는_DTO에서_막히지_않는다() throws Exception {
        doReturn(1L).when(studentService).signUpStudent(any());
        doReturn(new Token("token")).when(tokenService).createToken(1L, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("Abc12")))
                .andExpect(status().isOk());
    }

    @DisplayName("밑줄을 허용한다 — 통합 규칙이 geekchat 문자셋을 받아들였다")
    @Test
    void 밑줄을_허용한다() throws Exception {
        doReturn(1L).when(studentService).signUpStudent(any());
        doReturn(new Token("token")).when(tokenService).createToken(1L, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("abc_12")))
                .andExpect(status().isOk());
    }

    @DisplayName("3자를 허용한다 — 최소 길이가 4에서 3으로 넓어졌다")
    @Test
    void 세글자를_허용한다() throws Exception {
        doReturn(1L).when(studentService).signUpStudent(any());
        doReturn(new Token("token")).when(tokenService).createToken(1L, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("ab1")))
                .andExpect(status().isOk());
    }

    @DisplayName("전부 숫자면 DTO에서 막는다 — memberId와 혼동되는 값이다")
    @Test
    void 전부_숫자는_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("12345")))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("허용되지 않는 문자는 DTO에서 막는다")
    @Test
    void 하이픈은_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("abc-12")))
                .andExpect(status().isBadRequest());
    }
}
```

> `StudentController.signUpStudent`는 `200 OK` + `LoginResponse`를 반환한다(`ResponseEntity.ok(...)`, `origin/develop` 확인). `201`이 아니다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :course:test --tests '*SignUpUsernameContractTest*'`
Expected: 대문자가 `@Pattern`에 걸려 `400`

- [ ] **Step 3: 세 DTO의 정규식을 바꾼다**

세 파일 모두 `username` 필드의 `@Pattern`을 이렇게 바꾼다. **`regexp`는 대문자를 허용해야 한다** — 정규화가 도메인에서 일어나므로 DTO는 "정규화하면 규칙에 맞는가"를 봐야 한다.

```java
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}$",
                message = "아이디는 3~20자의 영문자·숫자·밑줄이어야 하며 영문자를 최소 하나 포함해야 합니다. 대문자는 소문자로 저장됩니다."
        )
```

> **왜 DTO 정규식이 도메인과 다른가.** 도메인은 **정규화 후** 값을 보므로 소문자만 받고, DTO는 **정규화 전** 입력을 보므로 대문자를 허용해야 한다. 둘은 미러이되 같은 문자열이 아니다 — 이 차이를 주석으로 남겨라.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :course:test --tests '*SignUpUsernameContractTest*'`
Expected: PASS

- [ ] **Step 5: 반증한다**

DTO 정규식을 옛 값(`^(?=.*[a-zA-Z])[a-zA-Z0-9]{4,20}$`)으로 되돌려 테스트가 **빨간불**인지 확인하고 원복한다.

- [ ] **Step 6: 4모듈 빌드 + 커밋**

```bash
./gradlew :common:build :course:build :notification:build :app:build
git add course/src
git commit -m "feat(member): 가입 요청 DTO의 username 정규식을 통합 규칙의 미러로"
```

---

## Task 4: 레거시 해시 소거 — 마이그레이션이 아니라 오너 SQL 1회

**⚠ 이 Task는 코드를 쓰지 않는다. 계획이 바뀌었다.**

### 왜 바뀌었나

초안은 레거시 계정 6건을 Flyway `V4`로 하드 삭제하려 했다. 그 전에 실측 추론이 나왔다:

```
member 7 = STUDENT 4 + TUTOR 3   (DB 실측)
bcrypt 1 = sehui2026 (STUDENT)   (오너 인증 검증 때 신규 가입)
→ 레거시 6 = STUDENT 3 + TUTOR 3 — 강사 전원이 레거시다
→ 다른 강사가 없으므로 강의도 전부 그들 소유다
```

즉 **삭제하면 강의·수강신청·스케줄까지 연쇄로 사라진다.** 계획의 `BLOCKED` 조건에 착수 즉시 걸리는 상태였다.

**그런데 폐기는 목적이 아니라 수단이었다.** 목적은 **레거시 해시를 없애는 것**이고(그래야 ADR-0003 결정 4의 `{sha256}` 매처를 걷어낸다), 계정을 지우는 것은 그 수단 중 하나일 뿐이다.

**오너 결정(2026-08-26): 비밀번호 재설정.** 6계정에 새 bcrypt 해시를 넣으면 `legacy = 0`이 되어 같은 목적을 달성하고, 강의·수강신청 테스트 데이터가 그대로 남는다.

### 그래서 이 Task에서 없어진 것

- ~~`V4__purge_legacy_accounts.sql`~~ — 불필요
- ~~`MigrationV4Test`~~ — 불필요
- ~~종속 행 선삭제·고아 검사~~ — 지우는 게 없으니 고아도 없다
- ~~`BLOCKED`: TUTOR 포함 시 중단~~ — 해소됨

**소프트 삭제 + `unique` 함정 분석은 폐기되지 않는다** — 계획 ②에서 `account` 백필 시 `member`의 소프트 삭제 행을 어떻게 다룰지에 그대로 적용된다. 그 계획서로 옮긴다.

### 남는 것 — 오너 실행 SQL

- [ ] **Step 1: 새 비밀번호의 bcrypt 해시를 만든다**

애플리케이션과 **같은 인코더·같은 강도**여야 한다. `Password.hashPassword`가 무엇을 쓰는지 열어 확인하고(ADR-0003 결정 4는 BCrypt(12)를 명시한다), 그 설정으로 해시를 생성한다.

생성은 코드로 한다 — 온라인 bcrypt 생성기에 비밀번호를 넣지 마라.

```java
// course 테스트에서 1회 실행해 출력만 사용한다. 커밋하지 않는다.
System.out.println(new BCryptPasswordEncoder(12).encode("<새-비밀번호>"));
```

- [ ] **Step 2: 오너가 Supabase에서 실행한다**

```sql
-- 레거시(비-bcrypt) 계정 6건에 새 해시를 넣는다.
-- ⚠ 이 SQL은 UPDATE다. 실행 전 대상을 먼저 확인할 것.

-- 2-a. 대상 확인 (읽기 전용)
SELECT member_id, username, role
FROM ssa.member
WHERE password NOT LIKE '$2%'
ORDER BY role, member_id;

-- 2-b. 적용 — <BCRYPT_HASH> 자리에 Step 1의 출력을 넣는다
UPDATE ssa.member
SET password = '<BCRYPT_HASH>'
WHERE password NOT LIKE '$2%';

-- 2-c. 검증 — legacy가 0이어야 한다
SELECT COUNT(*) FILTER (WHERE password LIKE '$2%')     AS bcrypt,
       COUNT(*) FILTER (WHERE password NOT LIKE '$2%') AS legacy
FROM ssa.member;
```

> **모든 계정에 같은 비밀번호를 넣는 것은 테스트 계정이라 허용된다.** 실사용자가 0명이고 전부 오너 소유다. 실사용자가 있는 시스템이라면 계정별로 다른 임시 비밀번호 + 강제 변경이어야 한다.

- [ ] **Step 3: `{sha256}` 매처 제거는 이 계획의 범위 밖이다**

`legacy = 0`이 확인되면 ADR-0003 결정 4의 `DelegatingPasswordEncoder` `{sha256}` 매처와 `Member.login`의 승급 경로를 걷어낼 수 있다. **다만 그건 인증 코드 변경이라 계획 ③(인증 통합)에 속한다.** 여기서는 "제거 가능해졌다"는 사실만 기록한다.

> 지금 제거하면 안 되는 이유: 배포 전에 오너가 SQL을 안 돌렸는데 코드가 먼저 나가면, 레거시 해시를 가진 계정이 **로그인 자체를 못 한다.** 순서가 SQL 먼저다.

## Task 5: 계약 문서 갱신과 클라이언트 통보

**Files:**
- Modify: `orchestration/API-CONTRACT.md` §2 (BE 소유 구역)

- [ ] **Step 1: §2의 Bean-Validation 미러를 갱신한다**

현재 §2에 이렇게 있다:
```
- `username`: `^(?=.*[a-zA-Z])[a-zA-Z0-9]{4,20}$`
```

새 규칙과 **정규화 동작**을 함께 적는다. 정규화를 안 적으면 클라이언트가 대문자를 클라이언트 단에서 거부해 버린다.

- [ ] **Step 2: 클라이언트 동시 갱신 목록을 오케스트레이터에 통보한다**

| 위치 | 조치 |
|---|---|
| 웹 `app/signup/page.tsx:84` | zod 정규식을 DTO 미러(`^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}$`)로 |
| 웹 `lib/chat-bridge.ts` | **삭제 대상**이지만 이 계획의 범위가 아니다. 규칙 통일로 lowercase 매핑이 무의미해진다는 것만 통보 |
| Android signup 검증 | 위와 동일 |

- [ ] **Step 3: 커밋 (계약 문서는 별도 저장소가 아니므로 통보만)**

---

## 자기 검토 결과

**스펙 커버리지:** 설계안 §3(username 정책) 전부 — 규칙(Task 1·3), 유니크(기존 제약 유지, `account` 도입은 계획 ②), 정규화(Task 1·2), 3쌍 병합(계획 ②의 백필에서), 전환기 제약(계획 ②). 오너 승인 2건 중 폐기는 Task 4, 규칙은 Task 1~3.

**범위 밖으로 명시한 것:** `account` 테이블, 듀얼라이트, `T9`, JWT 통일, `chat-bridge.ts` 삭제, `_ssa` 폴백 계정 조회 — 전부 계획 ②·③.

**자기 검토에서 고친 것:** Task 3 Step 1에 처음에는 항상 참인 단정을 넣고 "구현자가 다시 써라"고 적었다. **그건 자리표시자다** — 계획서 규칙 위반이고, 통과하지만 아무것도 검증하지 않는 테스트를 만드는 형태다. `StudentController`의 실제 반환(`200 OK` + `LoginResponse`)을 확인해 제대로 된 케이스 5개로 교체했다.

**BLOCKED 조건 해소:** 초안은 "폐기 대상에 TUTOR가 포함되면 중단"을 조건으로 뒀는데, 실측 추론(`member 7 = STUDENT 4 + TUTOR 3`, `bcrypt 1 = sehui2026`)으로 **착수 즉시 걸리는 상태**임이 드러났다. 그래서 조건을 기다리지 않고 오너 결정을 먼저 받았고, **폐기 대신 비밀번호 재설정**으로 바뀌어 조건 자체가 사라졌다.

**계획 ②로 이월한 분석:** 소프트 삭제 + `unique` 충돌(`MemberEntity`의 `@SQLDelete` × `username unique`, `V3`와 같은 구조)은 이 계획에서 쓰이지 않지만 `account` 백필 시 그대로 적용된다.
