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
- ✅ `user` 모듈 + 공유 base 엔티티 → common — commit `08b8afa`
- ✅ ChatError 매핑 + ErrorResponse → `common/presentation/web` (step 5 일부 선행, auth 차단 해소) — commit `c1c6dc7`
- ✅ `auth` 모듈 (RefreshToken, OAuthClient, AuthService, Auth/DevAuthController, Jwt*, SecurityConfig, OAuthClientAdapter, RefreshToken 영속) — `TokenService` 포트 추출로 AuthService→infrastructure 결합 해소, `refreshTokenExpiryDays`는 `@Value` 주입. 131 green
- ✅ `room` 모듈 (ChatRoom/ChatRoomMember/ChatRoomType/InviteLink, ChatRoom/InviteLink Repository, ChatRoom/InviteLink Service, Room/InviteLink Controller+Dto, RoomExpirationScheduler, room 영속) — `WebSocketBroadcaster.joinRoom` 직접 호출을 `RoomMemberJoined`(room/domain/event) 이벤트로 대체. **동기 `@EventListener`로 처리(타이밍 동일, sync→async 아님)** — 인메모리 세션 등록은 트랜잭션 커밋 순서 제약이 없으므로 안전. 131 green
- ✅ `chat` 모듈 (Message/MessageType, MessageRepository(+PaginationDirection), MessageService, MessageDto, MessageExpirationScheduler, Message 영속) — commit `99877cf`
- ✅ `websocket` 모듈 (WebSocketBroadcaster 포트, Handler/EventListener/WsOutMessage, BroadcasterAdapter/SessionManager, WebSocketConfig) — commit `8a1fca0`
- ✅ AppProperties + HealthController → `common` — commit `5dbca8c`
- ✅ `ChatEvent` sealed class 분할 → chat/room/websocket per-module 이벤트 — commit `657bbb8`
- ✅ chat↔room **순환 제거**: GET messages → chat `MessageController` 이동; RoomExpirationScheduler 메시지 정리 → chat `RoomExpiryMessageCleanup`(RoomExpired 리스너, room→chat 제거) — commit `d0b37e7`
- ✅ 6개 모듈 `@ApplicationModule(type=OPEN)` + `ModularityTests.verify()` 통과(순환 0) — commit `d0b37e7`

✅ **Phase 3 완료** — 72파일을 6개 기능 모듈(common/user/auth/room/chat/websocket)로 재배치, 옛 레이어 패키지 0, `verify()` green, **132 tests green**.
후속 정제(선택): OPEN → CLOSED + `@NamedInterface` API 노출 + 명시적 allowedDependencies로 방향 강제.

### 문서화된 타협 (test 수정)
- `AuthServiceTest`/`AuthServiceOAuthTest`: AuthService 생성자 인자 변경(plumbing) — `appProperties` → `appProperties.jwt.refreshTokenExpiryDays`(Long), `jwtTokenProvider`는 `TokenService` 구현체로 그대로 전달. 단언(assertion) 로직 무변경, 이동 파생 import 추가만.
- `ChatRoomServiceTest`/`InviteLinkServiceTest`: `WebSocketBroadcaster` mock → `ApplicationEventPublisher` mock + 생성자 인자 교체(plumbing). `InviteLinkServiceTest`의 단언 1개만 의미 변환 — `verify { webSocketBroadcaster.joinRoom("u2","r1") }` → `verify { eventPublisher.publishEvent(RoomMemberJoined("u2","r1")) }` (결합 해소가 broadcaster seam을 제거하므로 동일 의도의 최소 번역). `ChatRoomServiceTest`는 joinRoom 단언 없음 → 순수 plumbing.

## Phase 4 — 관리자 역할/권한 모델 (AI 게이팅 선결) ✅ 완료 (PR #5, main `9758d5c`)

- `User.role: UserRole(USER/ADMIN)` 도메인 + DB 컬럼 + 매핑 (`withRole`/`isAdmin`)
- JWT에 role claim 포함 → `JwtAuthenticationFilter`가 `ROLE_<role>` authority 부여, `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`
- 비로그인/일반/소셜 = 사람:사람 채팅만, ADMIN = AI 채팅 가능. `DevAuthController` dev-login에 `admin` 파라미터
- 검증: `AdminGatingIntegrationTest` (권한별 접근) green

## Phase 5 — Spring AI 2.0 (관리자 게이팅 + 모델 교체 가능) ✅ 완료 (branch `migration/phase5-spring-ai`)

구현된 범위 (사용자 결정: **여러 provider 동시 wiring + config 선택**):
- **`:ai` Gradle 기능 모듈** (Phase 6 패턴) — `geekchat.kotlin-library`, `:common`에만 의존, `@ApplicationModule(type=OPEN)`
- 모듈 내 헥사고날: `application/port/out/AiChatPort` + `infrastructure/SpringAiChatAdapter` + `application/service/AiChatService` + `presentation/web/AiController`
- provider 3종 starter 동시 wiring(anthropic+openai+ollama) — 활성 provider는 `spring.ai.model.chat`(`AI_CHAT_PROVIDER`, 기본 anthropic) config로 선택. 코드 변경 없이 교체
- **키 없이도 부팅**: `SpringAiChatAdapter`가 `ObjectProvider<ChatModel>`로 호출 시점 해석. provider 미구성(키 없음)이면 anthropic 자동구성이 백오프 → ChatModel 없음 → "not configured" 응답. 휴먼 채팅 무영향
  - 주의: `@ConditionalOnBean(ChatModel)`을 component-scan 빈에 쓰면 스캔 시점에 자동구성 ChatModel이 미등록 → 조건 항상 false 버그. ObjectProvider 지연 해석으로 회피
- 게이트: `@PreAuthorize("hasRole('ADMIN')")` (Phase 4 role). `POST /api/ai/chat`
- 빌드: spring-ai BOM을 `:ai`+`:app` 양쪽 import (io.spring.dependency-management 버전은 project 경계로 전파 안 됨) · 라이브러리 모듈에 `junit-platform-launcher` 추가(engine/launcher 정렬)
- 검증: 139 tests green (`AiChatGatingTest` admin 200 / 일반 403 / 익명 401 + `SpringAiChatAdapterTest` fallback) · `:app:bootRun`(local) `/health` ok + `/api/ai/chat` 토큰 없이 401

미구현(후속/선택): `MessageType.AI` 영속 + system user · 스트리밍(SSE/WS 브리지) · 별도 feature flag(현재 "ChatModel 존재 = 활성"으로 대체). AI 응답은 REST 동기 반환.

## Phase 6 — Gradle 물리 멀티모듈 분리 ✅ 완료 (PR #4, main `e821945`)

사용자 결정: **레이어별이 아닌 기능별** 서브프로젝트(Phase 3 Modulith 모듈과 1:1).
- 서브프로젝트: `:common :user :auth :room :chat :websocket :app`
- `build-logic` composite build + precompiled 컨벤션 플러그인: `geekchat.kotlin-library`(plain jar) · `geekchat.spring-boot-app`(bootJar)
- 의존 그래프(project): user→common · auth→common,user · room→common,user · chat→common,user,room · websocket→common,auth,room,chat · app→전부
- **`:app`만 bootJar** — 6개 모듈 jar를 `BOOT-INF/lib/*-0.0.1-SNAPSHOT.jar` nested로 번들
- 모듈별 Spring starter 분배(web/security/data-jpa/validation/websocket/jjwt/spring-tx)
- 검증: 132 tests green · bootJar nested 확인 · `:app:bootRun`(local) `/health` ok · CI green
- 후속: 전체 테스트가 `:app`에 집중 → 모듈별 src/test 재배치; starter `api` vs `implementation` 정밀화

## Phase 7 — Spring HATEOAS (표현 계층) ✅ 완료 (branch `migration/phase7-hateoas`)

`spring-boot-starter-hateoas`(HATEOAS 3.1.1, Boot 4.1 BOM 관리) 추가. 서비스/도메인 무변경, 표현 계층만.

구현 범위 (**계약 보존 우선** — 프론트 JSON 계약을 깨지 않음):
- `RoomModelAssembler`(:room)로 링크 중앙화 — 단일 room 응답을 `EntityModel`로 래핑. 필드는 `@JsonUnwrapped`로 최상위 유지 + `_links`만 추가(순수 additive). 링크: `self`(/api/rooms/{id}) · `mute` · `invite-link` — 모두 :room 컨트롤러(RoomController/InviteLinkController), 모듈 경계 준수.
- 적용: `POST /api/rooms`(생성) + `POST /api/invite/{code}/join`(초대 참여) — 둘 다 단일 room.
- **컬렉션(GET /api/rooms)은 plain 배열 유지**: `CollectionModel`은 배열→`_embedded` 객체로 바꿔 기존 계약/테스트(`$[0].id`)를 깬다. 또한 HAL 메시지 컨버터가 `RepresentationModel` 타입 한정이라 bare `List<EntityModel>`은 `_links`를 렌더하지 않음(실측). → 컬렉션 per-element 링크는 의도적으로 보류.
- 검증: 141 tests green. `RoomHateoasTest`(생성 응답 `_links` self/mute/invite-link + 필드 보존 / 리스트 `_embedded` 없음) → Boot 4.1 + Jackson 3 HAL 직렬화 실증.

부수 수정 (test 인프라): `application-dev.yml`의 `ddl-auto: update` override 제거. `@ActiveProfiles("test","dev")`에서 dev의 update가 test의 create-drop을 덮어쓰는데, H2+MySQLDialect에서 update가 스키마를 안정 생성 못 해 "다른 테스트가 먼저 mem:testdb를 채워주길 기대"하는 실행-순서 의존 결함이 잠복해 있었다(새 테스트 클래스 추가로 표면화). 제거 후 모든 통합 컨텍스트가 create-drop으로 자체 스키마 생성 → 순서 독립.

## Phase 8 — 배포: Render(무료) + Supabase(Postgres) ✅ 설정 완료 (branch `migration/render-supabase-postgres`)

친구 클라우드(VPN 내부, self-hosted runner 필요) 방식을 **취소**하고 Render 무료 호스팅으로 전환 (사용자 결정 2026-07-10).

- **DB: MySQL → PostgreSQL 마이그레이션** — Render 무료엔 관리형 MySQL이 없음. 순수 설정 변경(드라이버 `org.postgresql`, `PostgreSQLDialect`, jdbc URL). 엔티티/쿼리 무변경 근거: 모든 `@Query`가 JPQL, enum=`EnumType.STRING`, ID=UUID 문자열, `@GeneratedValue`/네이티브 SQL 없음. 테스트는 H2 `MODE=PostgreSQL`.
- **DB 호스트: Supabase**(무료 Postgres) — 세션 pooler(5432) + SSL, env 주입. (무료: 500MB~1GB, 1주 유휴 시 일시정지.)
- **앱 호스트: Render 무료 Docker web service** — `render.yaml`(Blueprint) + `Dockerfile` 멀티모듈용 재작성(`MaxRAMPercentage`로 512MB 적응). 첫 배포는 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 빈 DB에 스키마 생성.
- **런북**: `docs/DEPLOY-RENDER.md`. 친구 클라우드 자산(`docker-compose.yml`·`deploy/`·`docs/DEPLOYMENT.md`)은 보존(참조용).
- **검증(실측)**: 141 tests green(Postgres dialect/H2-PG) · `docker build` 성공(멀티모듈 이미지) · 로컬 Postgres `bootRun` `/health`={db:connected} + 방 생성 `_links` 실동작.
- **남은 수동 단계(사용자 계정 필요)**: Supabase 프로젝트 생성 → Render Blueprint 연결 → 시크릿(DB_*, FRONTEND_URL, OAUTH_CALLBACK_URL) 입력. `docs/DEPLOY-RENDER.md` 참조.
- 제약: Render 무료 512MB(빠듯, 필요 시 JAVA_OPTS 튜닝/유료 $7) + 15분 spin-down. 실 서비스엔 유료 DB 권장.

---

## 진행 상태

- ✅ 코드베이스 매핑 (`.planning/codebase/` 8개 문서)
- ✅ 타깃 스택 조사 + Boot 경로 결정 (Boot 4.1 + AI 2.0)
- ✅ **Phase 0 완료** — PR #1 main 머지 (`f6ec672`), 130 tests green
- ✅ **Phase 2 완료** — Boot 4.1.0 + Kotlin 2.3.21 + Jackson 3 (PR #2 main 머지)
- ✅ **Phase 3 완료** — Spring Modulith 6개 기능 모듈 + `verify()` green (PR #3 `208a8eb`)
- ✅ **Phase 6 완료** — Gradle 멀티모듈(기능별 7 서브프로젝트) + nested-jar bootJar (PR #4 `e821945`), 132 tests green
- ✅ **Phase 4 완료** — 관리자 role + JWT authority + `@PreAuthorize` 게이팅 (PR #5 `9758d5c`)
- ✅ **Phase 5 완료** — `:ai` 모듈 + Spring AI 2.0 (provider 3종 config 선택, admin 게이팅, 키 없이 부팅), 139 tests green (PR #6 `aef03f7`)
- ✅ **Phase 7 완료** — Spring HATEOAS 3.1.1, 단일 room 응답 `EntityModel` additive 래핑 + 링크 중앙화, 141 tests green
- ✅ **Phase 8 설정 완료** — 배포를 **Render 무료(Docker) + Supabase Postgres**로 전환(친구 클라우드/VPN 취소). DB MySQL→Postgres 마이그레이션, `render.yaml` + `docs/DEPLOY-RENDER.md`. 실제 배포는 사용자 계정 수동 단계(Supabase/Render).
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
