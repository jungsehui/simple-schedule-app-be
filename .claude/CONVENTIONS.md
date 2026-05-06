# CONVENTIONS — 코드 / 테스트 / 커밋 패턴

이 파일은 "이렇게 해라"의 모음이다. 신규 코드 작성 시 이 패턴을 따른다.

## 1. Kotlin 코드 스타일

### 1.1 명명
- 클래스: `PascalCase` (`UserService`, `ChatRoomMember`)
- 함수: `camelCase` (`findByUsername`, `withMarkReadAt`)
- 상수: `UPPER_SNAKE_CASE` (`MAX_MEMBERS = 100`)
- 패키지: `lowercase.dot.separated`
- 도메인 → JPA 엔티티 접미사: `JpaEntity` (`UserJpaEntity`)
- 어댑터: `Adapter` (`UserRepositoryAdapter`)

### 1.2 도메인 모델
```kotlin
data class User(
    val id: String,
    val nickname: String,
    val status: UserStatus = UserStatus.ACTIVE,
) {
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    fun anonymize(): User = copy(
        nickname = "deleted_user_${id.take(8)}",
        status = UserStatus.WITHDRAWN,
        updatedAt = Instant.now(),
    )
}
```
- `data class` + `val`만
- 변경은 `copy()` 또는 `with*()` 메서드로
- 검증은 service 레이어에서 (도메인은 invariant만)

### 1.3 JPA 엔티티
```kotlin
@Entity
@Table(name = "users")
class UserJpaEntity(
    id: String = UUID.randomUUID().toString(),
    @Column(nullable = false) var nickname: String = "",
    // ...
) : SoftDeletableJpaEntity(id, ...) {

    fun toDomain(): User = User(id = id, nickname = nickname, ...)

    companion object {
        fun fromDomain(user: User): UserJpaEntity = UserJpaEntity(
            id = user.id, nickname = user.nickname, ...
        )
    }
}
```
- `class` + `var` (Hibernate proxy 호환)
- 모든 컬럼 round-trip 매핑 (RoundTripMappingTest로 검증)

### 1.4 서비스 메서드
```kotlin
@Transactional
fun signup(cmd: SignupCommand): Either<ChatError, TokenPair> {
    if (userRepository.existsByUsername(cmd.username)) {
        return Either.Left(ChatError.UsernameAlreadyTaken(cmd.username))
    }
    val user = userRepository.save(...)
    return Either.Right(issueTokenPair(user.id))
}
```
- `Either<ChatError, T>` 반환 (예외 던지지 않음)
- `@Transactional` 명시
- 검증 → 본 작업 → 이벤트 발행 순서

### 1.5 컨트롤러
```kotlin
@PostMapping("/signup")
fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<*> =
    authService.signup(request.toCommand()).fold(
        onLeft = { it.toResponseEntity() },
        onRight = { ResponseEntity.ok(TokenPairResponse(it.accessToken, it.refreshToken)) },
    )
```
- `@Valid + Bean Validation`
- `fold` 패턴으로 ResponseEntity 변환
- 도메인 직접 노출 금지, DTO `from()` factory 사용

## 2. 테스트 패턴

### 2.1 단위 테스트 (MockK)
```kotlin
class FooServiceTest {
    private val fooRepository = mockk<FooRepository>()
    private lateinit var service: FooService

    @BeforeEach
    fun setUp() {
        service = FooService(fooRepository)
    }

    @Test
    fun `methodName succeeds when condition`() {
        every { fooRepository.findById("x") } returns Foo(id = "x")
        val result = service.method("x")
        assertTrue(result.isRight)
    }

    @Test
    fun `methodName fails when condition`() {
        every { fooRepository.findById("x") } returns null
        val result = service.method("x")
        assertTrue(result.isLeft)
        assertTrue((result as Either.Left).value is ChatError.FooNotFound)
    }
}
```
- 한 테스트 = 한 시나리오. backtick으로 평문 이름.
- mock 정의는 위에, 호출은 명확히.

### 2.2 통합 테스트
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
class FooIntegrationTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `endpoint returns expected shape`() {
        mockMvc.post("/api/foo") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"x":"y"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { isNotEmpty() }
        }
    }
}
```

### 2.3 테스트 위치
- 단위: `src/test/kotlin/.../<같은 패키지>/<클래스명>Test.kt`
- 통합: `src/test/kotlin/.../integration/<클래스명>IntegrationTest.kt`
- 매핑: `src/test/kotlin/.../integration/RoundTripMappingTest.kt`에 추가

## 3. 커밋 규칙

### 3.1 메시지 형식
```
<type>: <subject>

<body — 무엇을, 왜>

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

### 3.2 type
| type | 의미 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `chore` | 빌드/설정/의존성 |
| `docs` | 문서 only |
| `test` | 테스트 추가/수정 |
| `refactor` | 기능 동일, 구조 변경 |
| `perf` | 성능 개선 |

### 3.3 subject 규칙
- 50자 이내
- 한글 가능. 동사 명령형 ("로그인 흐름 추가" 또는 "add login flow")
- 마침표 X

### 3.4 atomic commit
- 한 commit = 한 의미 단위
- 도메인 변경 + 테스트 추가 = 같은 commit
- 서로 무관한 변경 = 별개 commit

## 4. 파일 추가 시 체크리스트

새 도메인 모델/엔드포인트/이벤트 추가 시:
- [ ] 도메인 모델 (`domain/model/`)
- [ ] JPA 엔티티 + 매핑 (`adapter/out/persistence/entity/`)
- [ ] Repository 포트 (`application/port/out/`)
- [ ] Spring Data + 어댑터 (`adapter/out/persistence/`)
- [ ] 서비스 메서드 (`application/service/`)
- [ ] 컨트롤러 + DTO (`adapter/in/web/`)
- [ ] 단위 테스트 (서비스)
- [ ] 통합 테스트 (컨트롤러)
- [ ] 라운드트립 테스트 (도메인↔JPA)
- [ ] `docs/API.md` 또는 `docs/WEBSOCKET.md` 업데이트
- [ ] `CLAUDE.md` 도메인 용어집 업데이트 (필요 시)

## 5. 의존성 추가 규칙

`build.gradle.kts`에 새 의존성 추가 시:
1. **이유 commit message에 명시** (왜 이 라이브러리)
2. 가능한 한 표준 라이브러리/Spring Boot starter 우선
3. 추가 후 빌드 + 테스트 통과 확인
4. 라이센스 호환성 체크 (Apache 2 / MIT / BSD OK, GPL 주의)

## 6. 응답 / 커뮤니케이션

- 한국어 응답. 코드/에러 메시지는 영문.
- 결론 먼저, 근거는 간결하게.
- 변경 사항은 표로 리포트.
- 3개 이상 파일 동시 수정 시 계획 보여주고 승인.
- 에러 2번 연속 시 자동 멈춤 + 상황 리포트.
