# GeekChat Server v2

Kotlin/Spring Boot 기반 실시간 채팅 서버.

기존 NestJS + MikroORM + Socket.io 서버를 Kotlin + Spring Boot 3.4 + JPA + Raw WebSocket으로 마이그레이션.
Spring MVC + Virtual Threads(Java 21)로 비동기 프레임워크 없이 10k+ 동시 연결을 처리하며,
Render 무료 티어(512MB)에 최적화된 JVM 설정(SerialGC, 256MB 힙)을 적용.

## Tech Stack

| 항목 | 기술 |
|---|---|
| Language | Kotlin 2.0, Java 21 |
| Framework | Spring Boot 3.4, Spring MVC |
| Concurrency | Virtual Threads |
| Database | PostgreSQL + JPA/Hibernate 6 |
| WebSocket | Raw WebSocket (TextWebSocketHandler) |
| Auth | JWT (jjwt) + Spring Security |
| Architecture | Pragmatic Hexagonal |
| Test | JUnit 5, MockK, MockMvc, H2 |
| Deploy | Docker, Render |

## Local Development

```bash
# Prerequisites: Java 21

# Run with dev profile (H2 in-memory DB)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests
./gradlew test

# Build JAR
./gradlew bootJar
```

## Docker

```bash
# Build
docker build -t geek-chat-server-v2 .

# Run with H2 (dev)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JWT_SECRET=dev-secret-key-must-be-at-least-32-characters-long \
  geek-chat-server-v2

# Health check
curl http://localhost:8080/health
```

## REST API

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /auth/dev-login?name=X | No | Dev-only login |
| POST | /auth/refresh | No | Refresh token (rotation) |
| GET | /auth/me | Yes | Current user info |
| POST | /auth/logout | No | Logout |
| GET | /api/users/search?q=X | Yes | Search users |
| PATCH | /api/users/me/username | Yes | Set username |
| GET | /api/rooms | Yes | List my rooms |
| POST | /api/rooms | Yes | Create room |
| GET | /api/rooms/:id/messages | Yes | Messages (cursor pagination) |
| GET | /health | No | Health check |

## WebSocket

Connect: `ws://host/ws?token=JWT`

| Event | Direction | Payload |
|---|---|---|
| send_message | Client -> Server | `{ type, data: { roomId, content, clientMessageId } }` |
| message_ack | Server -> Client | `{ type, data: { clientMessageId, serverId } }` |
| new_message | Server -> Room | `{ type, data: { id, roomId, senderId, content, type, createdAt } }` |
| typing_start | Client -> Server | `{ type, data: { roomId } }` |
| typing_indicator | Server -> Room | `{ type, data: { roomId, userId } }` |
| mark_read | Client -> Server | `{ type, data: { roomId, lastReadMessageId } }` |
| read_update | Server -> Room | `{ type, data: { roomId, userId, lastReadAt } }` |

## Architecture

```
com.geekchat.server/
├── domain/           # Pure Kotlin models, events, errors (no JPA)
├── application/      # Use cases, port interfaces
├── adapter/
│   ├── in/web/       # REST controllers
│   ├── in/websocket/ # WebSocket handler
│   └── out/persistence/ # JPA entities, repositories
└── infrastructure/   # Security, config
```

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| DATABASE_URL | Yes | PostgreSQL JDBC URL |
| JWT_SECRET | Yes | Min 32 chars |
| FRONTEND_URL | No | CORS origin (default: http://localhost:3000) |
| PORT | No | Server port (default: 8080) |
| SPRING_PROFILES_ACTIVE | No | dev, test, or default |
