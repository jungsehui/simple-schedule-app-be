# GeekChat Server v2

Kotlin/Spring Boot 기반 실시간 채팅 서버.

기존 NestJS + MikroORM + Socket.io 서버를 Kotlin + Spring Boot + JPA + Raw WebSocket으로 마이그레이션.
Spring MVC + Virtual Threads(Java 21)로 비동기 프레임워크 없이 다수의 동시 연결을 처리한다.

> 모듈러 모놀리스 전면 마이그레이션(Boot 4.1 + Spring Modulith + Spring AI + HATEOAS) 진행 중 —
> 로드맵과 진행 상태는 [`docs/MIGRATION.md`](docs/MIGRATION.md) 참고.

## Tech Stack

| 항목 | 기술 |
|---|---|
| Language | Kotlin 2.0, Java 21 |
| Framework | Spring Boot 3.4, Spring MVC |
| Concurrency | Virtual Threads |
| Database | MySQL 8.0 (utf8mb4) + JPA/Hibernate 6 |
| WebSocket | Raw WebSocket (TextWebSocketHandler) |
| Auth | JWT (jjwt) + Spring Security |
| Architecture | Pragmatic Hexagonal |
| Test | JUnit 5, MockK, H2 (MODE=MySQL), Testcontainers |
| Deploy | Docker Compose + Nginx (TLS) |

## Profiles

| 프로필 | DB | ddl-auto | 용도 |
|---|---|---|---|
| (default) | MySQL | validate | 프로덕션. 스키마는 사람이 관리 |
| `local` | MySQL (docker-compose.dev.yml) | update | 로컬 실행(자기완결: datasource+secret 포함) |
| `dev` | (상속) | update | JPA 개발 시맨틱. 통합 테스트와 공유 |
| `test` | H2 (MODE=MySQL) | create-drop | JVM 테스트 |

## Local Development

```bash
# Prerequisites: Java 21, Docker

# 0. (최초 1회) git 훅 활성화 — push 전에 `./gradlew build`(컴파일+테스트+조립)를 자동 실행해
#    깨진 코드가 origin/CI에 올라가는 것을 차단한다. (긴급 우회: git push --no-verify)
./gradlew installGitHooks

# 1. 로컬 의존성(MySQL) 기동 — host 3310
docker compose -f docker-compose.dev.yml up -d

# 2. 앱 실행 (local 프로필: 위 MySQL에 자동 연결, env 설정 불필요)
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 헬스체크
curl http://localhost:8080/health
# {"status":"ok","db":"connected",...}

# 테스트 (H2, 컨테이너 불필요)
./gradlew test

# 의존성 기동 종료 (데이터 보존)
docker compose -f docker-compose.dev.yml down
```

> 3310 포트가 점유 중이면 `docker-compose.dev.yml`의 포트 매핑과 `DB_PORT`를 함께 바꾼다.

## Docker (production image)

프로덕션 스택(app + MySQL + Nginx/TLS)은 `docker-compose.yml` + `Dockerfile`을 사용한다.
상세 배포 절차는 [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md), 빠른 참조는 [`deploy/README.md`](deploy/README.md).

```bash
cp .env.docker.example .env.docker   # 시크릿 채우기 (JWT_SECRET 필수)
docker compose --env-file .env.docker up -d --build
```

## API 계약

정식 카탈로그는 다음 문서가 SSOT다:

- REST: [`docs/API.md`](docs/API.md)
- WebSocket: [`docs/WEBSOCKET.md`](docs/WEBSOCKET.md)
- DB 스키마: [`docs/DATABASE.md`](docs/DATABASE.md)
- 아키텍처: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

WebSocket 연결: `ws[s]://host/ws?token=<JWT>`

## Architecture

```
com.geekchat.server/
├── domain/           # Pure Kotlin models, events, errors (no JPA)
├── application/      # Use cases, port interfaces
├── adapter/
│   ├── in/web/       # REST controllers
│   ├── in/websocket/ # WebSocket handler
│   └── out/persistence/ # JPA entities, repositories
└── infrastructure/   # Security, config, schedulers
```

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **Yes (prod)** | ≥ 32 bytes. 미설정 시 prod 부팅 실패(fail-fast). local/test는 프로필이 자체 제공 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | prod | DB 접속 정보 |
| `FRONTEND_URL` | No | CORS origin (default: http://localhost:3000) |
| `FRONTEND_ORIGIN_PATTERNS` | No | 추가 CORS 패턴 (CSV, default: https://*.vercel.app) |
| `OAUTH_CALLBACK_URL` | OAuth | 양 provider 공통 콜백 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth | Google |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | OAuth | Naver |
| `PORT` | No | Server port (default: 8080) |
| `SPRING_PROFILES_ACTIVE` | No | `local` / `dev` / `test` / 비움(prod) |
