# RULES — 절대 규칙 + 코드 패턴

이 파일은 모든 변경 전 1번 읽고 시작한다. 위반은 자동 차단되거나 PR에서 거부된다.

## 🚫 절대 금지 (자동 차단)

| # | 규칙 | 이유 | 우회 |
|---|---|---|---|
| 1 | `domain/`에 `jakarta.persistence` / `org.springframework` import | 도메인은 프레임워크 모름 (헥사고날 핵심) | JPA는 `adapter/out/persistence/entity/`에 별도 + `toDomain/fromDomain` 매핑 |
| 2 | `~/Work/geek-chat/geek-chat-server/` (v1 NestJS) 수정 | 참조 전용. 동기화 깨짐 | Read는 OK. 수정 필요 시 사용자 명시 승인 |
| 3 | 서비스에서 예외 던지기 | ROP 패턴 위반 | `Either<ChatError, T>` 반환 |
| 4 | `rm -rf /` 또는 `~`, `git push --force main`, `git reset --hard` (확인 없이), `--no-verify`, `docker compose down -v` | 데이터/히스토리 파괴 | 정말 필요하면 `OMC_CONFIRM_DESTRUCTIVE=1` env 설정 |
| 5 | 시크릿 코드/.env*에 commit | git history 영구. 누출 시 회수 어려움 | `.env.docker`(gitignored) 또는 호스트 env var |
| 6 | 테스트 실패한 채로 commit | 안전망 깨짐 | `./gradlew test` 통과 필수 |

위반 시: 즉시 멈춤 → 위반 위치 보고 → 우회 필요하면 사용자 승인.

---

## ✍️ 코드 패턴 (이렇게 써라)

### 도메인 모델
```kotlin
data class User(val id: String, val nickname: String, ...) {
    fun anonymize(): User = copy(nickname = "deleted_user_${id.take(8)}", ...)
}
```
- `data class` + `val`. 변경은 `copy()` 또는 `with*()`.
- 검증은 service 레이어 (도메인은 invariant만).

### JPA 엔티티
```kotlin
@Entity @Table(name = "users")
class UserJpaEntity(...) : SoftDeletableJpaEntity(...) {
    fun toDomain(): User = User(id = id, ...)
    companion object { fun fromDomain(u: User): UserJpaEntity = UserJpaEntity(...) }
}
```
- `class` + `var`. 모든 필드 round-trip 매핑 (`RoundTripMappingTest`로 검증).

### 서비스
```kotlin
@Transactional
fun signup(cmd: SignupCommand): Either<ChatError, TokenPair> {
    if (...) return Either.Left(ChatError.UsernameAlreadyTaken(...))
    val saved = userRepository.save(...)
    eventPublisher.publishEvent(ChatEvent.X(...))   // 트랜잭션 AFTER_COMMIT 브로드캐스트
    return Either.Right(issueTokenPair(saved.id))
}
```
- `Either<ChatError, T>` 반환 (예외 X). `@Transactional` 명시.

### 컨트롤러
```kotlin
@PostMapping("/signup")
fun signup(@Valid @RequestBody req: SignupRequest): ResponseEntity<*> =
    authService.signup(req.toCommand()).fold(
        onLeft = { it.toResponseEntity() },
        onRight = { ResponseEntity.ok(TokenPairResponse.from(it)) },
    )
```
- `@Valid + Bean Validation`. `fold` 패턴.

### 테스트 (단위)
```kotlin
class FooServiceTest {
    private val repo = mockk<FooRepo>()
    @Test fun `methodName succeeds when X`() {
        every { repo.findById("x") } returns Foo(...)
        val r = service.method("x")
        assertTrue(r.isRight)
    }
}
```

### 테스트 (통합)
```kotlin
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test", "dev")
class XIntegrationTest {
    @Autowired lateinit var mockMvc: MockMvc
    // ...
}
```

---

## 📁 헥사고날 import 방향

```
adapter/in  ──►  application  ──►  domain
adapter/out ──►  application/port  (구현)
infrastructure ── 자유 (config/security/scheduler)
```

- `adapter/in`이 `adapter/out` 직접 import 금지 → 포트 거치기
- `service`가 `adapter` 직접 import 금지

검증: `bash .claude/hooks/check-domain-imports.sh` (자동) 또는 `grep -r 'import com.geekchat.server.adapter' src/main/kotlin/com/geekchat/server/application/`.

---

## 📝 커밋

```
<type>: <subject>

<body — 무엇을, 왜>

Co-Authored-By: Claude <noreply@anthropic.com>
```

| type | 의미 |
|---|---|
| `feat` | 새 기능 | `fix` | 버그 수정 |
| `chore` | 빌드/설정 | `docs` | 문서 only |
| `test` | 테스트 | `refactor` | 구조만 |

- subject 50자 이내, 마침표 X
- atomic: 한 commit = 한 의미

---

## 📂 새 도메인/엔드포인트 추가 체크리스트

1. 도메인 모델 (`domain/model/`)
2. JPA 엔티티 + 매핑 (`adapter/out/persistence/entity/`)
3. 포트 (`application/port/out/`) → Spring Data + 어댑터
4. 서비스 (`application/service/`) — `Either<ChatError, T>` 반환
5. 컨트롤러 + DTO (`adapter/in/web/`)
6. 단위 + 통합 + round-trip 테스트
7. `docs/API.md` 또는 `WEBSOCKET.md` 업데이트
8. `./gradlew test` 통과

자세한 절차: `.claude/commands/add-rest-endpoint.md` / `add-domain-model.md`.

---

## 🌐 응답 / 커뮤니케이션

- 한국어 응답. 코드/에러 메시지는 영문.
- 결론 먼저, 변경 사항은 표로.
- 3+ 파일 동시 수정 시 계획 보여주고 승인.
- 에러 2번 연속 시 자동 멈춤 + 상황 리포트.
- 메시지 content를 로그에 출력 시 `// TODO: Remove content from log before production` 주석 필수.
