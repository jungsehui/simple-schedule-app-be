# username 규칙 통일과 레거시 계정 폐기 — 실행 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통합 username 규칙을 도메인 불변식으로 세우고, 비밀번호를 아무도 모르는 레거시 계정 6건을 참조 무결성을 지키며 폐기한다.

**Architecture:** username 규칙을 `Member` 도메인에 두고(현재는 요청 DTO에만 있다) 저장 전 소문자 정규화를 강제한다. 레거시 계정 폐기는 Flyway 마이그레이션으로 하되, 이 저장소는 애그리거트 간 참조가 **ID 참조라 DB FK 제약이 없으므로**(ADR-0004 Phase A) 종속 행을 명시적으로 함께 지운다.

**Tech Stack:** Spring Boot 4.1 / Java 21 / Gradle 멀티모듈 / Flyway(운영 전용) / PostgreSQL(운영)·MySQL(로컬)·H2(테스트)

## 이 계획의 범위와 범위 밖

ADR-0003 Stage 5는 독립 배포 가능한 세 덩어리다. **이 계획은 ① 하나만 다룬다.**

| | 내용 | 이 계획 |
|---|---|---|
| ① | 레거시 계정 폐기 + username 규칙 통일 | **여기** |
| ② | `account` 테이블 신설·백필·듀얼라이트 | 별도 계획 |
| ③ | JWT 통일 + `T9` + Spring Security 전환 | 별도 계획 |

①을 먼저 떼는 이유: **①이 유일하게 클라이언트 계약을 건드린다.** 큰 인증 변경과 묶으면 클라이언트 릴리스가 그것에 인질이 된다. 그리고 ① 이후에 `account`를 백필하면 깨끗한 데이터에서 시작한다.

## Global Constraints

- **통합 username 규칙(오너 승인): `^(?=.*[a-z])[a-z0-9_]{3,20}$`** — 소문자·숫자·밑줄만, 3~20자, 영문자 최소 1자
- **입력은 거부가 아니라 정규화한다** — 대문자 입력은 소문자로 변환해 저장한다
- **레거시 계정 판별 술어: `password NOT LIKE '$2%'`** — bcrypt가 아닌 것이 레거시다
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
| `app/src/main/resources/db/migration/V4__purge_legacy_accounts.sql` (신규) | 레거시 계정과 종속 행 폐기 |
| `app/src/test/.../MigrationV4Test.java` (신규) | V4를 Flyway API로 실행해 검증 |

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

## Task 4: 레거시 계정 폐기 마이그레이션 (V4)

**Files:**
- Create: `app/src/main/resources/db/migration/V4__purge_legacy_accounts.sql`
- Create: `app/src/test/java/com/example/simplescheduleapp/migration/MigrationV4Test.java`

**⚠ 이 Task의 위험:** 이 저장소는 애그리거트 간 참조가 **ID 참조라 DB FK 제약이 없다**(ADR-0004 Phase A — `LectureEnrollmentEntity`는 `@Column(name="student_id")`이지 `@JoinColumn`이 아니다). 즉 **회원 행만 지우면 수강신청이 조용히 고아가 된다.** DB가 막아 주지 않으므로 마이그레이션이 직접 순서를 지켜야 한다.

**하드 삭제인 이유:** `MemberEntity`는 `@SQLDelete`로 소프트 삭제인데 `username`에 `unique = true`가 걸려 있다. 소프트 삭제하면 **username 슬롯이 영구 점유**된다. `V3`가 정확히 같은 구조(`uk_pending_lecture_student`가 `deleted_date`를 포함하지 않음) 때문에 존재한다. 폐기 대상은 되살릴 이유가 없으므로 하드 삭제한다.

- [ ] **Step 1: 마이그레이션을 쓴다**

`app/src/main/resources/db/migration/V4__purge_legacy_accounts.sql`

```sql
-- V4: 레거시(비밀번호 미상) 계정 폐기 — ADR-0003 Stage 5 선행 (오너 승인 2026-08-26)
--
-- 대상: password가 bcrypt('$2'로 시작)가 아닌 회원. 무염 SHA-256 시절 계정이고
--       비밀번호를 아무도 모른다. 실사용자는 0명이며 전부 오너 테스트 계정이다.
--
-- ⚠ 하드 삭제인 이유: MemberEntity는 @SQLDelete로 소프트 삭제인데 username에 unique가
--   걸려 있다. 소프트 삭제하면 username 슬롯이 영구 점유돼 그 값을 다시 쓸 수 없다.
--   V3가 정확히 같은 구조(uk_pending_lecture_student가 deleted_date 미포함) 때문에 있다.
--
-- ⚠ 종속 행을 먼저 지우는 이유: 이 저장소는 애그리거트 간 참조가 ID 참조라
--   DB FK 제약이 없다(ADR-0004 Phase A). 회원만 지우면 수강신청이 조용히 고아가 되고
--   DB는 그걸 막아 주지 않는다.
--
-- 로그인 한 번으로 bcrypt 승급된 계정은 이 술어에서 자동으로 빠진다 —
-- 비밀번호를 기억해낸 계정은 폐기 대상이 아니라는 뜻이라 의도한 동작이다.
--
-- 조건부인 이유: 새 환경(빈 스키마)에서는 Flyway가 ddl-auto보다 먼저 돌아 테이블이 없다.
DO $$
DECLARE
    legacy_ids BIGINT[];
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'member'
    ) THEN
        RETURN;
    END IF;

    SELECT array_agg(member_id) INTO legacy_ids
    FROM member
    WHERE password NOT LIKE '$2%';

    IF legacy_ids IS NULL OR array_length(legacy_ids, 1) IS NULL THEN
        RETURN;
    END IF;

    -- 종속 행부터. 각 테이블이 실재할 때만 지운다(새 환경 대비).
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'lecture_enrollment') THEN
        DELETE FROM lecture_enrollment WHERE student_id = ANY(legacy_ids);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'pending_lecture_enrollment') THEN
        DELETE FROM pending_lecture_enrollment WHERE student_id = ANY(legacy_ids);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'special_lecture_enrollment') THEN
        DELETE FROM special_lecture_enrollment WHERE student_id = ANY(legacy_ids);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'consultation_attendee') THEN
        DELETE FROM consultation_attendee WHERE member_id = ANY(legacy_ids);
    END IF;

    -- JOINED 상속의 자식 테이블 → 부모 순서.
    DELETE FROM student WHERE member_id = ANY(legacy_ids);
    DELETE FROM tutor   WHERE member_id = ANY(legacy_ids);
    DELETE FROM parent  WHERE member_id = ANY(legacy_ids);
    DELETE FROM member  WHERE member_id = ANY(legacy_ids);
END $$;
```

> **구현자 확인 사항:** 위 자식 테이블명(`student`/`tutor`/`parent`)과 `consultation_attendee.member_id`는 엔티티에서 유도한 것이다. **실제 스키마명을 `information_schema`로 확인하고 다르면 맞춰라.** 그리고 **강사가 소유한 강의(`lecture.tutor_id`)는 이 목록에 없다** — 강사 계정을 지우면 그 강의가 고아가 된다. 폐기 대상 6건의 역할 분포를 먼저 확인하고, TUTOR가 포함돼 있으면 **BLOCKED로 보고하라.** 강의 폐기는 별도 결정이 필요하다.

- [ ] **Step 2: Flyway API로 검증하는 테스트를 쓴다**

Flyway는 운영에서만 활성이고 테스트는 H2 + `ddl-auto`다. 따라서 **마이그레이션을 Flyway API로 직접 실행**해야 검증된다(V2·V3가 같은 방식으로 검증됐다).

기존 마이그레이션 테스트가 있으면 그 패턴을 따르라: `git grep -l "Flyway" app/src/test course/src/test`로 찾아 **같은 구조로** 쓴다. 없으면 `org.flywaydb.core.Flyway`를 직접 구성해 H2에 대해 돌린다.

검증할 것:
1. 레거시 계정과 그 수강신청이 사라진다
2. **bcrypt 계정과 그 수강신청은 남는다** (이게 없으면 "전부 지웠다"와 구별되지 않는다)
3. 두 번 실행해도 결과가 같다 (멱등)

- [ ] **Step 3: 반증한다**

`DELETE FROM lecture_enrollment ...` 줄을 지우고 테스트가 **빨간불**인지 확인한다. 빨간불이 아니면 그 테스트는 고아 행을 안 보고 있는 것이다. 확인 후 원복한다.

- [ ] **Step 4: 운영 검증 SQL을 준비한다**

배포 후 오너가 실행할 전후 대조다. 계획서에 그대로 둔다.

```sql
-- 배포 전
SELECT COUNT(*) FILTER (WHERE password LIKE '$2%')     AS bcrypt,
       COUNT(*) FILTER (WHERE password NOT LIKE '$2%') AS legacy
FROM ssa.member;

-- 배포 후 — legacy가 0이어야 하고 bcrypt는 그대로여야 한다
-- (같은 질의)

-- 고아 검사 — 전부 0이어야 한다
SELECT 'lecture_enrollment' AS t, COUNT(*) FROM ssa.lecture_enrollment e
  WHERE NOT EXISTS (SELECT 1 FROM ssa.member m WHERE m.member_id = e.student_id)
UNION ALL
SELECT 'pending_lecture_enrollment', COUNT(*) FROM ssa.pending_lecture_enrollment p
  WHERE NOT EXISTS (SELECT 1 FROM ssa.member m WHERE m.member_id = p.student_id)
UNION ALL
SELECT 'special_lecture_enrollment', COUNT(*) FROM ssa.special_lecture_enrollment s
  WHERE NOT EXISTS (SELECT 1 FROM ssa.member m WHERE m.member_id = s.student_id);
```

- [ ] **Step 5: 4모듈 빌드 + 커밋**

```bash
./gradlew :common:build :course:build :notification:build :app:build
git add app/src
git commit -m "feat(migration): V4 레거시 계정 폐기 (종속 행 포함, 하드 삭제)"
```

---

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

**BLOCKED 조건:** Task 4에서 폐기 대상에 TUTOR가 포함되면 강의가 고아가 되므로 중단하고 보고한다.
