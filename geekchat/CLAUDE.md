# GeekChat Server v2 — AI Context Hub

이 파일은 모든 Claude 세션이 코드베이스를 재탐색하지 않고 즉시 작업할 수 있도록 작성되었다.

## 1. 프로젝트 개요

**GeekChat Server v2**는 실시간 채팅 서버이다. 1:1 대화, 그룹 채팅, 임시 방, 자동 삭제 메시지, 초대 링크 같은 X(트위터) 스타일 프라이버시 기능을 제공한다.
v1(NestJS)을 Kotlin/Spring Boot + JPA + Raw WebSocket으로 마이그레이션했다(현재 Boot 4.1.0).

- **백엔드**: 이 저장소 (Kotlin/Spring Boot)
- **프론트엔드**: `~/Work/geek-chat/geek-chat-web-v2/` (Next.js 15, 별도 저장소, Vercel 배포)
- **v1 NestJS 서버**: `~/Work/geek-chat/geek-chat-server/` (참조 전용, 절대 수정 금지 — OAuth 코드 포팅의 원본)
- **v1 Expo Web 프론트**: `~/Work/geek-chat/geek-chat-web/` (참조 전용)

## 2. 기술 스택

| 영역 | 기술 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21, Java 21 (Virtual Threads) |
| 프레임워크 | Spring Boot 4.1.0, Spring MVC, Spring Security |
| 영속 | JPA / Hibernate 7, PostgreSQL (운영: Supabase 관리형, `geekchat` 스키마 / 로컬: postgres:16) |
| WebSocket | Spring WebSocket (Raw, no STOMP) |
| 인증 | JWT (jjwt 0.12.5), BCrypt (Spring Security) |
| 빌드 | Gradle 8.12 (Kotlin DSL) |
| 테스트 | JUnit 5, MockK 1.13.12, H2(MODE=PostgreSQL) |
| 배포 | Docker Compose, Nginx (TLS termination), Let's Encrypt |
| 프로필 | `default`(prod, validate, PostgreSQL) / `local`(update, 로컬 PostgreSQL) / `dev`(show-sql만) / `test`(create-drop, H2) |

## 3. 디렉토리 맵

```
geek-chat-server-v2/
├── CLAUDE.md                ← 이 파일
├── AGENTS.md                ← 에이전트 호출/코드 규칙
├── README.md                ← 사용자용 개요
├── docs/
│   ├── ARCHITECTURE.md      ← 헥사고날 레이어 + 의존도
│   ├── API.md               ← REST 엔드포인트 카탈로그
│   ├── WEBSOCKET.md         ← WS 이벤트 카탈로그
│   ├── DEPLOYMENT.md        ← 친구 서버 배포 런북
│   └── DATABASE.md          ← 스키마 문서 (MySQL 기준으로 작성된 옛 문서, 현재 DB는 PostgreSQL)
├── deploy/                  ← 배포 자산 (nginx.conf 등)
├── build.gradle.kts
├── Dockerfile               ← 멀티 스테이지 (jdk-builder + jre-runtime)
├── docker-compose.yml       ← app + mysql + nginx (옛 자체 호스팅 구성, 현재 앱 설정과 맞지 않음. 운영 배포는 루트 `deploy/docker-compose.prod.yml`)
├── .env.docker.example
└── src/
    ├── main/
    │   ├── kotlin/com/geekchat/server/
    │   │   ├── domain/                ← 순수 Kotlin (JPA 의존 절대 금지)
    │   │   │   ├── model/             ← User, ChatRoom, Message, ChatRoomMember, InviteLink, RefreshToken, UserProvider
    │   │   │   ├── event/             ← ChatEvent sealed class
    │   │   │   └── error/             ← ChatError sealed + Either<L, R>
    │   │   ├── application/
    │   │   │   ├── port/out/          ← Repository / WebSocketBroadcaster / OAuthClient 인터페이스
    │   │   │   └── service/           ← AuthService, UserService, ChatRoomService, MessageService, InviteLinkService
    │   │   ├── adapter/
    │   │   │   ├── in/web/            ← REST 컨트롤러 + DTO + ChatErrorMapping
    │   │   │   ├── in/websocket/      ← ChatWebSocketHandler, SessionManager, BroadcasterAdapter, EventListener
    │   │   │   └── out/persistence/   ← JPA 엔티티 + Spring Data + 어댑터
    │   │   └── infrastructure/
    │   │       ├── config/            ← AppProperties, SecurityConfig, WebSocketConfig
    │   │       ├── security/          ← JwtTokenProvider, JwtAuthenticationFilter
    │   │       └── scheduler/         ← Room/Message Expiration
    │   └── resources/
    │       └── application*.yml       ← default / local / dev / test
    └── test/kotlin/com/geekchat/server/
        ├── domain/                    ← 순수 단위 테스트
        ├── application/service/       ← MockK 기반
        └── integration/               ← @SpringBootTest + H2(MODE=PostgreSQL)
```

## 4. 헥사고날 레이어 규칙 (절대 규칙)

| 레이어 | 의존 가능 | 의존 금지 | 비고 |
|---|---|---|---|
| `domain/` | (없음) | Spring, JPA, Jackson, 외부 라이브러리 | 순수 Kotlin data class만 |
| `application/port/` | `domain/` | adapter, infrastructure | 인터페이스만 |
| `application/service/` | `domain/`, `application/port/`, Spring `@Service`/`@Transactional` | adapter, infrastructure | 비즈니스 로직 |
| `adapter/in/*` | `application/`, `domain/` | 다른 adapter | controller/handler |
| `adapter/out/*` | `application/port/`, `domain/` | 다른 adapter | port 구현 |
| `infrastructure/` | 자유 | (제한 없음) | config/security/scheduler |

**위반 시 import 정렬 단계에서 발견되어야 함.** 위반 발견 시 즉시 수정하고 ArchUnit 테스트 추가 검토.

## 5. 도메인 용어집

| 용어 | 정의 | 코드 위치 |
|---|---|---|
| **User** | 채팅 사용자. `passwordHash`(로컬) 또는 `UserProvider`(OAuth) 보유. `status: ACTIVE/WITHDRAWN`. | `domain/model/User.kt` |
| **UserProvider** | OAuth 연동 정보. (provider, providerId) UNIQUE. 한 User에 여러 provider 가능. | `domain/model/UserProvider.kt` |
| **ChatRoom** | 대화방. `type: DIRECT(1:1, 최대 2명) / GROUP(최대 100명)`. `expiresAt?` 있으면 임시 방. | `domain/model/ChatRoom.kt` |
| **ChatRoomMember** | (user, room) 멤버십. `lastReadAt`로 읽음 표시 추적. | `domain/model/ChatRoomMember.kt` |
| **Message** | 메시지. `clientMessageId` UUID 멱등성 키. `expiresAt?` 있으면 자동 삭제. | `domain/model/Message.kt` |
| **InviteLink** | 8자 코드 초대 링크. TTL + 사용 횟수 제한. | `domain/model/InviteLink.kt` |
| **RefreshToken** | 14일 opaque UUID. rotation 방식 (사용 즉시 삭제 → 재발급). | `domain/model/RefreshToken.kt` |
| **ChatEvent** | 도메인 이벤트 sealed class. ApplicationEventPublisher로 발행 → ChatEventListener에서 WS 브로드캐스트. | `domain/event/ChatEvent.kt` |
| **ChatError** | 모든 에러 타입 sealed class. 서비스 메서드는 `Either<ChatError, T>` 반환. | `domain/error/ChatError.kt` |
| **Either** | 자체 구현 ROP 모나드 (Arrow 미사용). | `domain/error/Either.kt` |

## 6. 핵심 컨벤션 (꼭 지킬 것)

1. **서비스는 예외 던지지 않는다.** `Either<ChatError, T>` 반환만. 컨트롤러에서 `fold`로 ResponseEntity 변환.
2. **도메인 → JPA 매핑**: JPA 엔티티에 `toDomain()` 인스턴스 메서드 + `fromDomain()` companion. 도메인은 JPA를 모른다.
3. **JPA 엔티티는 `class` + `var`**. 도메인 모델은 `data class` + `val`. `with*` 메서드로 불변 업데이트.
4. **Soft delete**: 메시지/방은 `@SQLRestriction("deleted_at IS NULL")`. **유저는 `status` enum 사용** (조인 깨짐 방지).
5. **WebSocket 브로드캐스트**: `ApplicationEventPublisher.publishEvent(ChatEvent.X)` → `ChatEventListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`)에서 처리. 트랜잭션 커밋 전 발송 방지.
6. **JWT subject = userId**. JwtAuthenticationFilter가 SecurityContext에 userId(String)를 principal로 세팅. 컨트롤러에서 `@AuthenticationPrincipal userId: String`로 받음.
7. **`@field:NotBlank`/`@field:Pattern`**으로 request DTO 검증. Bean Validation 사용.
8. **멱등성**: 메시지 전송은 `clientMessageId` UNIQUE 제약 + 사전 조회 + race-condition catch 3중 방어.
9. **테스트 작성 시**: 단위는 MockK, 통합은 `@SpringBootTest @ActiveProfiles("test", "dev")` + H2(MODE=PostgreSQL). geekchat에는 Testcontainers 테스트가 없다.
10. **로그에 메시지 content 포함 시**: `// TODO: Remove content from log before production` 주석 필수.

## 7. 빌드/실행/테스트 명령

```bash
# 의존성/빌드
./gradlew build                      # 빌드 + 모든 테스트
./gradlew bootJar                    # 프로덕션 JAR 생성

# 테스트
./gradlew test                       # 전체 테스트 (현재 91개)
./gradlew test --tests "*AuthServiceTest*"

# 로컬 실행 (local 프로필 — 자기완결: datasource+secret 포함, env 불필요)
docker compose -f docker-compose.dev.yml up -d          # PostgreSQL 16 (host 5433)
./gradlew bootRun --args='--spring.profiles.active=local'
# 주: dev 프로필은 통합 테스트(@ActiveProfiles "test","dev")와 공유하는 JPA 시맨틱 전용 —
#     로컬 실행엔 local 사용. 5433 점유 시 docker-compose.dev.yml 포트 + DB_PORT 동시 변경.

# Docker
docker compose --env-file .env.docker up -d --build
docker compose logs -f app
docker compose down
```

## 8. 프로필 의미

| 프로필 | DB | ddl-auto | 용도 |
|---|---|---|---|
| (default) | PostgreSQL (Supabase, `currentSchema=geekchat`, SSL require) | **validate** | 프로덕션. 스키마는 사람이 관리 |
| `local` | PostgreSQL 16 (`docker-compose.dev.yml`, host 5433) | **update** | 로컬 실행. 자기완결(datasource 포함) + show-sql |
| `dev` | (지정 안 함) | (지정 안 함) | show-sql만. 통합 테스트(`test`,`dev`)와 공유 |
| `test` | H2 in-memory (MODE=PostgreSQL) | **create-drop** | JVM 테스트. 빠른 부팅 |

실제 PostgreSQL에 대한 방언 검증 테스트는 아직 없다. 필요하면 SSA의 `ScheduleOverlapQuery*Test`(Testcontainers) 패턴을 참고한다.

## 9. 주요 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (없음) | local / dev / test / 비움(prod) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | localhost/5432/postgres/(없음)/(없음) | 앱이 읽는 DB 자격증명 (local 프로필은 5433/geekchat/geekchat/geekchat) |
| `DB_SCHEMA` / `DB_SSLMODE` | geekchat / require | 접속 스키마와 SSL 모드 |
| `DB_ROOT_PASSWORD` | (없음) | 옛 `docker-compose.yml`의 mysql 서비스만 사용 |
| `JWT_SECRET` | dev fallback | ≥32자 |
| `FRONTEND_URL` | http://localhost:3000 | CORS origin (단일) |
| `FRONTEND_ORIGIN_PATTERNS` | https://*.vercel.app | 추가 origin 패턴 (CSV) |
| `OAUTH_CALLBACK_URL` | http://localhost:8080/auth/callback | 양 provider 공통 콜백 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | Google Cloud Console 발급 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | — | Naver Developers 발급 |

## 10. v1과의 관계

- v1(`~/Work/geek-chat/geek-chat-server/`)은 NestJS + MikroORM + Socket.io. **수정 금지**.
- v1 OAuth 흐름은 v2 트랙 B에서 포팅 중. v1의 `auth.controller.ts:259-317`이 token-exchange 로직, `auth.service.ts:56-135`가 3-tier 매칭 로직의 기준 코드.
- DB 스키마는 호환되도록 설계되었으나 v1↔v2 간 데이터 마이그레이션은 필요 없음 (별도 인프라).

## 11. 자주 보는 파일 (북마크)

- 인증: `application/service/AuthService.kt`, `infrastructure/security/JwtTokenProvider.kt`, `infrastructure/config/SecurityConfig.kt`
- 채팅: `application/service/ChatRoomService.kt`, `MessageService.kt`
- WebSocket: `adapter/in/websocket/ChatWebSocketHandler.kt`, `ChatEventListener.kt`
- 영속: `adapter/out/persistence/entity/*.kt`, `adapter/out/persistence/adapter/*.kt`
- 설정: `src/main/resources/application.yml`, `build.gradle.kts`
- 배포: `Dockerfile`, `docker-compose.yml`, `deploy/nginx.conf`

## 12. 작업 시 체크리스트

작업 종료 전 반드시:
- [ ] `./gradlew test` 모두 통과
- [ ] 새 도메인 모델/JPA 필드는 `toDomain`/`fromDomain` 양쪽 매핑
- [ ] 새 ChatError는 `ChatErrorMapping.toResponseEntity()` 매핑 (sealed when으로 컴파일러가 강제)
- [ ] 새 엔드포인트는 `docs/API.md`에 추가
- [ ] 새 WS 이벤트는 `docs/WEBSOCKET.md`에 추가
- [ ] 도메인 변경 시 `docs/DATABASE.md` 동기화
