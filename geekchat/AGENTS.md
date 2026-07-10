# AGENTS.md — 에이전트 호출 가이드 + 코드 규칙

이 파일은 Claude/Codex/Gemini 등 에이전트 도구들이 GeekChat v2에서 작업할 때 따라야 할 라우팅과 규칙을 정의한다.

## 1. 에이전트 라우팅 (요청 유형별)

| 요청 유형 | 권장 에이전트 / 스킬 |
|---|---|
| 코드베이스 탐색 ("X 함수 어디 있어?") | `Explore` 서브에이전트 |
| 새 기능 설계 ("임시 방 기능 추가") | `Plan` 서브에이전트 → 검토 → `executor` |
| 버그 수정 (재현 가능) | `oh-my-claudecode:debugger` 또는 `tdd` 스킬 |
| 리팩토링 (구조만 변경, 기능 동일) | `oh-my-claudecode:code-simplifier` |
| 보안 이슈 검토 | `oh-my-claudecode:security-reviewer` |
| 테스트 추가 | `oh-my-claudecode:test-engineer` 또는 `tdd` 스킬 |
| 문서 업데이트 | `oh-my-claudecode:writer` |
| 배포/인프라 변경 | 본 메인 에이전트 (또는 `executor`) |
| Spring Boot/JPA 사용법 모르겠음 | `oh-my-claudecode:document-specialist` (공식 docs) |

## 2. 작업 흐름 (3개 이상 파일 수정 시)

1. **분석**: `Explore` 에이전트로 영향 범위 파악
2. **계획 제시**: 변경 계획을 사용자에게 보여주고 승인 받음 (`ExitPlanMode`)
3. **구현**: 도메인 → 포트 → 어댑터 → 서비스 → 컨트롤러 → 테스트 순서
4. **검증**: `./gradlew test` (단위 + 통합) 모두 통과
5. **커밋**: 작업 단위가 명확하면 atomic commit (작은 단위 권장)

## 3. 코드 작성 절대 규칙

### 3.1 도메인 레이어
- `domain/` 어디에도 `import jakarta.persistence.*`, `import org.springframework.*` 절대 금지
- `data class` + `val` 만 사용. 변경은 `copy()` 또는 `with*()` 메서드
- `Instant`로 시간 다루기 (LocalDateTime 금지)
- 검증 실패 시 `IllegalArgumentException`이 아니라 서비스 레이어에서 `Either.Left(ChatError.X)`로 변환

### 3.2 서비스 레이어
- 모든 public 메서드는 `Either<ChatError, T>` 반환 (Unit 반환은 `Either<ChatError, Unit>`)
- `@Transactional`은 명시적으로 (메서드 단위)
- DB 변경 후 도메인 이벤트 발행: `eventPublisher.publishEvent(ChatEvent.X(...))`
- 순환 의존성 금지: 서비스 ↔ 어댑터 직접 참조 안 됨, 항상 포트를 거침

### 3.3 어댑터 레이어
- `adapter/out/persistence/entity/`: JPA 엔티티는 `class` + `var` (Hibernate proxy 호환)
- `toDomain()` / `fromDomain()` companion 매핑은 모든 필드 round-trip 보장 (RoundTripMappingTest로 검증)
- `adapter/in/web/dto/`: 요청/응답 DTO는 도메인 모델 직접 노출 금지. `from(domain)` factory 사용
- 어댑터는 다른 어댑터를 import 하지 않는다. 공통 로직은 `application/`로 빼거나 `infrastructure/`로 분리

### 3.4 테스트
- **테스트 파일은 스펙이다**. 실패 시 프로덕션 코드를 고치지, 테스트를 고치지 않는다 (단, 새 기능 추가로 테스트가 새로 필요한 경우 제외)
- 단위 테스트: MockK `mockk<>()`, `every {}`, `slot<>()`, `verify {}` 패턴
- 통합 테스트: `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test", "dev")`
- `application.yml` 변경 시 `test` 프로필도 함께 검토

## 4. 커밋 메시지 스타일

```
<type>: <subject>

<body — 왜 변경했는지>

Co-Authored-By: Claude <noreply@anthropic.com>
```

타입: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`. 기존 커밋(`df7d227`) 스타일 따르기.

## 5. 절대 하지 말 것

- 테스트 없이 새 기능 머지 (단위 + 통합 모두 추가)
- `Either` 우회해서 예외로 에러 처리
- 도메인에 JPA/Spring 의존성 추가
- `application.yml`에 시크릿 하드코딩 (env var로만)
- `git push --force` (단, `git-master` 에이전트가 명시적으로 안전 확인한 경우 제외)
- v1 NestJS 코드 수정 (`~/Work/geek-chat/geek-chat-server/`은 read-only)

## 6. 문서 업데이트 의무

다음 변경 시 반드시 해당 문서 동기화:

| 변경 | 업데이트할 문서 |
|---|---|
| 새 REST 엔드포인트 | `docs/API.md` |
| 새 WebSocket 이벤트 | `docs/WEBSOCKET.md` |
| 새 도메인 모델/필드 | `docs/DATABASE.md` + `CLAUDE.md`의 도메인 용어집 |
| 헥사고날 레이어 규칙 변경 | `docs/ARCHITECTURE.md` + `CLAUDE.md` |
| 배포 절차 변경 | `docs/DEPLOYMENT.md` |
| 환경 변수 추가 | `CLAUDE.md` 9절 + `.env.docker.example` |

## 7. 위임 시 프롬프트 작성 규칙

서브에이전트 호출 시:
- 컨텍스트(목적, 범위) 명확히
- 읽어야 할 파일 절대 경로 명시
- 결과 형식 지정 (테이블, 다이어그램 등)
- 응답 길이 제한 ("under 300 words")
- 단일 책임: 한 에이전트에 여러 일 시키지 말고 분할

## 8. 모델 선택 기준

| 작업 성격 | 권장 모델 |
|---|---|
| 단순 lookup, 짧은 답변 | haiku |
| 일반 구현/리팩토링 | sonnet (기본) |
| 아키텍처 설계, 복잡한 디버깅 | opus |
