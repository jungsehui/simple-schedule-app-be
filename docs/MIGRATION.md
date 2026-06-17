# MIGRATION — Modular Monolith 전면 마이그레이션 로드맵

> 멀티세션 작업의 단일 진실 소스(SSOT). `.planning/codebase/`(gitignore, clone 미포함)와 달리 추적됨.
> 시작: 2026-05-17 · 대상: `geek-chat-server-v2`

## 결정 로그

| 결정 | 값 | 근거 |
|---|---|---|
| Spring Boot 경로 | **Boot 4.1 + Spring AI 2.0 (최신)** | 사용자 선택 (2026-05-17). 최신 메이저. JakartaEE 11 + Framework 7 + Kotlin 2.2 동반 |
| 모듈화 방식 | Spring Modulith(패키지 모듈) 먼저 → Gradle 물리 멀티모듈은 선택적 후순위 | 패키지 모듈만으로 "모듈러 모놀리스" 달성. 물리 분리는 빌드 격리 필요 시 |
| AI provider | 미정 (Phase 5에서 config로 결정, 기본 Anthropic 가정) | Spring AI는 provider가 config 선택. Phase 5 전 API 키 provision 필요 |
| CI/CD | CI(테스트 게이트) 즉시, CD는 Phase 8 분리 | 호스트가 VPN 내부 + 80/443만 노출 → hosted runner 직접 접근 불가 |

## 타깃 버전 매트릭스 (구현 시 live BOM / start.spring.io 재확인 전제)

| 라이브러리 | 타깃 | 비고 |
|---|---|---|
| Spring Boot | **4.1.0** | Java 17+ (21 사용), Gradle ≥8.14 (8.14.5), Servlet 6.1/JakartaEE 11 |
| Spring Framework | 7.0.8 | Boot가 관리 |
| Kotlin | **2.3.21** | Boot 4.1.0 gradle.properties가 관리하는 버전(소스 확인). 연구의 2.2.x는 outdated |
| Gradle | 8.14.5 | Boot 4.1 최소 8.14 |
| Spring Modulith | 2.1.0 | 양 Boot 호환 (Phase 3) |
| Spring HATEOAS | 3.1.x | starter로 BOM 관리 (Phase 7) |
| Spring AI | 2.0.0 | Boot 4 전용. `spring-ai-starter-model-{anthropic,openai,ollama}` (Phase 5) |

---

## 사전 작업 (사용자 수동 — 지난 세션 보안 스레드, 미완)

- [ ] **repo PUBLIC→PRIVATE 전환 확인** — 지난 세션에 직접 실행하기로 했으나 확인 안 됨. `gh repo view geek-chat/server-v2 --json visibility`
- [ ] **노출된 인프라 자격 로테이션** — VPN peer endpoint + SSH user가 git 히스토리(`9595aae`~`3736144` 이전)에 잔존. visibility와 무관하게 로테이션 권장
- HEAD는 `3736144`에서 리댁션 완료 (placeholder 마스킹)

---

## Phase 0 — 배포 가능 + 안전 바닥 (아키텍처 변경 X)

목표: 새 contributor가 `./gradlew bootRun --args='--spring.profiles.active=dev'`로 즉시 실행. 테스트 게이트 존재. HIGH 보안 구멍 차단.

- [ ] **0.1 프로필 분리** — `application.yml`(prod) + `application-dev.yml` + `application-test.yml` (순수 리팩터, 동작 보존)
- [ ] **0.2 로컬 dev Docker** — `docker-compose.dev.yml` (MySQL만, 로컬 포트, nginx/TLS 없음). prod `docker-compose.yml`/`Dockerfile`은 그대로 유지
- [ ] **0.3 bootRun 문서화** — README/CLAUDE.md에 dev 실행 절차
- [ ] **0.4 보안 HIGH 3종**:
  - [ ] `dev-login`을 `@Profile("dev")`로 — prod 빈 그래프에서 제외 (`AuthController.kt:171-189`, `AuthService.kt:333-374`)
  - [ ] JWT secret 기본값 제거 → 미설정 시 fail-fast (`AppProperties.kt:15`, `application.yml`)
  - [ ] 메시지 `content`를 `ChatEvent.MessageSent`에서 제거 → listener가 DB 재조회 (`ChatEvent.kt`, `ChatEventListener.kt:29`, `MessageService.kt:103`) ※ **0.6 테스트 선행 후**
- [ ] **0.5 prod 안정화** — `lazy-initialization: false` (prod), `@Async` 바운디드 executor 빈
- [ ] **0.6 characterization 테스트(고위험 경로)** — 리팩터 안전망. OAuth 상태머신(LoggedIn/LinkingRequired/SignupRequired), WS 이벤트/브로드캐스트 흐름, 스케줄러. **0.4의 content 제거보다 먼저**
- [ ] **0.7 GitHub Actions CI** — `.github/workflows/ci.yml`: PR·push에 `./gradlew test`

검증 게이트: `./gradlew test` green + `bootRun(dev)` 정상 기동 + dev-login이 prod 프로필에서 404.

## Phase 1 — characterization 테스트 보강 (Boot 4 업그레이드 前 안전망)

Boot 4 = 메이저 업그레이드라 0.6을 넘어 회귀 위험 경로를 더 덮는다. 기존 112 테스트 + 신규로 OAuth/WS/스케줄러/컨트롤러 검증 두께 확보.

검증 게이트: 테스트 카운트 증가 + 전부 green.

## Phase 2 — Spring Boot 3.4.1 → 4.1.0 업그레이드 ✅ 완료

실제 적용 (Boot 4.1.0 / Kotlin 2.3.21 / Gradle 8.14.5):
- 래퍼 8.12 → 8.14.5 (Boot 4 최소 8.14)
- build.gradle: Boot 4.1.0, Kotlin 2.3.21
- **브레이킹 1 — Framework 7**: `UriComponentsBuilder.fromHttpUrl` 제거 → `fromUriString` (`AuthController` OAuth authorize URL 2곳)
- **브레이킹 2 — test slice 분리**: `@AutoConfigureMockMvc`가 `spring-boot-starter-webmvc-test`로 이동 → 의존성 추가 + import `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (통합테스트 3개)
- **브레이킹 3 — Jackson 2 → 3 (가장 큼)**: Boot 4 기본이 Jackson 3(`tools.jackson`). 앱의 `ObjectMapper`/`JsonNode` 주입·사용을 J3로 이전:
  - import `com.fasterxml.jackson.databind.{ObjectMapper,JsonNode}` → `tools.jackson.databind.*` (main 3 + test 4 파일)
  - kotlin 모듈 `com.fasterxml.jackson.module` → `tools.jackson.module:jackson-module-kotlin`
  - `application.yml`: 제거된 `serialization.write-dates-as-timestamps` → `spring.jackson.use-jackson2-defaults: true` (ISO 날짜 유지)
  - `JsonNode.asText()`/`asLong()`/`asBoolean()`는 J3에도 그대로 존재 → 코드 변경 불필요. `jjwt-jackson`은 자체 J2 사용(격리, 무충돌)
  - `spring-boot-jackson2`(compat) 시도는 dual-config 바인딩 충돌로 폐기

검증: 컴파일 + **130 tests green** + bootRun(local) 실연.

## Phase 3 — Spring Modulith 패키지 기반 모듈화 (진행 중)

**확정 구조 (사용자 승인)**: 최상위 = 기능 모듈, 내부 = 레이어. 각 모듈이 linkareer `activity` 서비스처럼
`domain/{model,event,repository(포트)} · application/{dto,port/{in,out},service} · infrastructure/{config,persistence/{entity,repository,adapter,mapper},security,oauth,scheduler} · presentation/{web}` 구조. `adapter/adapter` 중첩 제거.

**모듈**: `common`(범용) · `user` · `auth`(→user) · `room` · `chat`(→room) · `websocket`. Modulith는 최상위 하위 패키지를 모듈로 인식.

**MSA 노트**: 모듈러 모놀리스 = 인-프로세스. auth→user는 네트워크 아님(공개 API 직접 호출/이벤트). 경계 규율 지키면 후일 MSA 추출 시 이벤트→브로커 externalize, named interface→API 계약으로 전환.

### 모듈 배정 맵 (72 main 파일)
| 모듈 | 파일 |
|---|---|
| **common** ✅ | error/{Either,ChatError}. (예정: config/AppProperties, persistence/entity/{Base,SoftDeletable}JpaEntity, presentation/web/{HealthController,ChatErrorMapping}) |
| **user** | model/{User,UserProvider,UserStatus,AuthProvider}, repository/{UserRepository,UserProviderRepository}, service/UserService, web/{UserController,UserDto}, persistence/{User,UserProvider}{JpaEntity,SpringData,Adapter} |
| **auth** (→user) | model/RefreshToken, repository/RefreshTokenRepository, port/out/OAuthClient, service/AuthService, web/{AuthController,DevAuthController,AuthDto}, security/{JwtTokenProvider,JwtAuthenticationFilter}, config/SecurityConfig, oauth/OAuthClientAdapter, persistence/RefreshToken* |
| **room** | model/{ChatRoom,ChatRoomMember,ChatRoomType,InviteLink}, repository/{ChatRoomRepository,InviteLinkRepository}, service/{ChatRoomService,InviteLinkService}, web/{RoomController,InviteLinkController,RoomDto,InviteLinkDto}, scheduler/RoomExpirationScheduler, persistence/{ChatRoom,ChatRoomMember,InviteLink}* |
| **chat** (→room) | model/{Message,MessageType}, repository/MessageRepository, service/MessageService, web/MessageDto, scheduler/MessageExpirationScheduler, persistence/Message* |
| **websocket** | port/out/WebSocketBroadcaster, {ChatWebSocketHandler,ChatEventListener,WebSocketBroadcasterAdapter,WebSocketSessionManager}, config/WebSocketConfig |
| **이벤트 분할** | `ChatEvent` → chat(MessageSent/Read/Burned/Expired) · room(RoomExpiring/Expired) · presence(UserConnected/Disconnected). websocket이 `@ApplicationModuleListener`로 구독 |

### 결합 위반 3종 (이동 중 해소)
- `AuthService→infrastructure(JwtTokenProvider,AppProperties)` → `TokenService` 포트 추출
- `ChatRoomService→WebSocketBroadcaster.joinRoom` → `RoomMemberJoined` 이벤트 (⚠️ sync→async, Phase 0.6 WS 테스트가 회귀 감지)
- 공유 `ChatEvent` → 모듈별 분리

### 진행
- ✅ 3.1 Modulith 2.1.0 추가 (Boot 4.1 호환 확인) + ModularityTests probe — commit `d5f34f4`
- ✅ `common` 모듈 (Either, ChatError) — commit `d5f34f4`
- ⬜ user → auth → room → chat → websocket (각 green+커밋) → ChatEvent 분할 → `ModularityTests.verify()` 활성화

검증 게이트: `ApplicationModules.verify()` 통과 + 전체 테스트 green + bootRun(local).

## Phase 4 — 관리자 역할/권한 모델 (AI 게이팅 선결)

- `User.role: UserRole(USER/ADMIN)` 도메인 + DB 컬럼 + 매핑
- `JwtAuthenticationFilter`가 GrantedAuthority 부여, `@PreAuthorize("hasRole('ADMIN')")`
- 비로그인/일반/소셜 = 사람:사람 채팅만, ADMIN = AI 채팅 가능

검증 게이트: 권한별 접근 통합 테스트.

## Phase 5 — Spring AI 2.0 (관리자 게이팅 + 모델 교체 가능)

- `AiChatPort` (application/port/out) + `SpringAiChatAdapter` (adapter/out/ai) — `OAuthClient` 포트 패턴 답습
- `ChatClient`/`ChatModel` 주입, provider는 config (`spring.ai.{anthropic,openai,ollama}`), provider별 프로필
- `MessageType.AI` + system user, `AiResponseHandler` `@ApplicationModuleListener`
- 게이트: `@PreAuthorize("hasRole('ADMIN')")` + feature flag (`AiFeatureProperties`)
- 스트리밍: SSE 또는 WS 브로드캐스트 브리지

검증 게이트: 비-admin 차단 + provider 교체(config만)로 동작 + AI 응답 영속.

## Phase 6 — [선택] Gradle 물리 멀티모듈 분리

Modulith 체감 후 사용자 결정. `domain`/`application`/`adapter-persistence`/`adapter-web`/`app` 서브프로젝트, `libs.versions.toml`, `build-logic` convention plugin. `app`만 bootJar, 나머지 plain jar.

## Phase 7 — Spring HATEOAS (표현 계층, 최저위험)

DTO를 `EntityModel`/`CollectionModel`로 래핑, `RepresentationModelAssembler`로 링크 중앙화. 서비스 변경 없음.

## Phase 8 — 배포 자동화 / CD

VPN 내부 호스트 대응: self-hosted runner(VPN 내부) 또는 pull 기반 배포. 스키마는 Flyway/Liquibase로 (현 `ddl-auto` 부트스트랩 풋건 제거). prod Docker 강화.

---

## 진행 상태

- ✅ 코드베이스 매핑 (`.planning/codebase/` 8개 문서)
- ✅ 타깃 스택 조사 + Boot 경로 결정 (Boot 4.1 + AI 2.0)
- ✅ **Phase 0 완료** — PR #1 main 머지 (`f6ec672`), 130 tests green
- ✅ **Phase 2 완료** — Boot 4.1.0 + Kotlin 2.3.21 + Jackson 3, 130 tests green (브랜치 `migration/phase2-boot4`)
  - (Phase 1 characterization 보강은 Phase 0.6에서 충분히 커버되어 별도 진행 생략)
  - ✅ 0.1 프로필 분리 (`application.yml` + `-dev` + `-test` + `-local`) — commit `c76aad7`
  - ✅ 0.2 `docker-compose.dev.yml` (MySQL 3310) — commit `c76aad7`
  - ✅ 0.4a dev-login → `@Profile("dev","local")` DevAuthController (prod 부재) — commit `975480e`
  - ✅ 0.4b JWT secret fail-fast (기본값 제거 + ≥32B 검증) — commit `975480e`
  - ✅ 0.7 GitHub Actions CI (`.github/workflows/ci.yml`) — commit `f7fd58b`
  - ✅ 0.3 bootRun 문서화 (README 정정 + CLAUDE §7) — commit `5da108c`
  - ✅ 0.5a prod `lazy-initialization: false` / local `true` — commit `5da108c`
        · 0.5b: `@Async`는 VT 활성화로 이미 virtual-thread executor 사용 → 바운디드 풀 불필요.
          WS send-path의 `synchronized` 카리어-핀닝은 perf phase로 이관
  - ✅ 0.6 characterization 테스트 — AuthServiceOAuthTest(8) + ChatEventListenerTest(10),
        스케줄러 테스트 audit=적정(이벤트 발행+선택 검증됨) — commit `d554128`
  - ✅ 0.4c **재분류**: `content`는 이벤트→broadcast 경로(클라이언트 전달용)이며 **로그에 절대 안 찍힘**
        (`grep` 검증). "Remove content from log" TODO 2개는 오해 주석 → 정정. 이벤트/PII 디커플링은
        Phase 3(ChatEvent 분할)로 이관 — Phase 0에서 제거 시 불필요한 DB 재조회만 늘고 보안 이득 0

> 참고: 로컬 실행 프로필은 `dev`가 아닌 **`local`** (`dev`는 통합 테스트와 공유하는 JPA 시맨틱 전용).
> 실행: `docker compose -f docker-compose.dev.yml up -d` → `./gradlew bootRun --args='--spring.profiles.active=local'`

## 핵심 원칙 (마이그레이션 내내)

- 한 번에 한 Phase. 각 Phase 끝에 green 테스트 + 커밋. 깨진 채 다음 Phase 금지
- 테스트는 스펙 — 실패 시 src 수정 (테스트 파일 수정은 사용자 승인 후)
- 헥사고날 레이어 규칙 유지 (domain은 JPA/Spring 미의존)
- 시크릿/인프라 토폴로지 코드·문서·커밋 금지
