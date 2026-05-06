# Architecture

## 1. 아키텍처 스타일: Pragmatic Hexagonal

```
                    ┌──────────────────────────────────┐
                    │  adapter/in (Web, WebSocket)     │
                    └──────────────┬───────────────────┘
                                   │ uses
                    ┌──────────────▼───────────────────┐
                    │  application/                    │
                    │  ┌─────────────┐ ┌─────────────┐│
                    │  │ port/in     │ │ port/out    ││
                    │  └──────┬──────┘ └──────▲──────┘│
                    │         │               │       │
                    │  ┌──────▼─────────────  │ ──────┐│
                    │  │       service/        │      ││
                    │  └───────────────────────┘      ││
                    └──────────────┬───────────────────┘
                                   │ depends on
                    ┌──────────────▼───────────────────┐
                    │  domain/ (pure Kotlin)           │
                    │   model/ event/ error/           │
                    └──────────────────────────────────┘
                                   ▲
                                   │ implements port/out
                    ┌──────────────┴───────────────────┐
                    │  adapter/out (Persistence, OAuth)│
                    └──────────────────────────────────┘
                                   │
                    ┌──────────────▼───────────────────┐
                    │  infrastructure/ (config/sec)    │
                    └──────────────────────────────────┘
```

## 2. import 방향 표

| from \\ to | domain | port | service | adapter/in | adapter/out | infrastructure |
|---|---|---|---|---|---|---|
| **domain** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **port** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **service** | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **adapter/in** | ✅ | ✅ | ✅ | (자기) | ❌ | ✅ (security) |
| **adapter/out** | ✅ | ✅ | ❌ | ❌ | (자기) | ✅ |
| **infrastructure** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 3. 데이터플로우: 메시지 전송 (HTTP는 불가, WebSocket만)

```
1. Client                  ws.send({type:"send_message", data:{...}})
                                       │
2. ChatWebSocketHandler    handleTextMessage() — 라우팅
                                       │
3. ChatWebSocketHandler    handleSendMessage() — 데이터 검증
                                       │
4. MessageService          sendMessage() @Transactional
                           ├─ findByClientMessageId() ← idempotency check
                           ├─ existsByUserIdAndChatRoomId() ← 멤버십
                           ├─ messageRepository.save(Message)
                           ├─ chatRoomRepository.save(room.withLastMessageAt())
                           └─ eventPublisher.publishEvent(ChatEvent.MessageSent)
                                       │
5. ChatWebSocketHandler    sendToSession(session, "message_ack")  ← 발신자에게
                                       │
                          (트랜잭션 커밋)
                                       │
6. ChatEventListener       @Async @TransactionalEventListener(AFTER_COMMIT)
                           onMessageSent() — 모든 멤버에게 broadcast
                                       │
7. WebSocketBroadcaster    broadcastToRoom(roomId, "new_message")
                                       │
8. WebSocketSessionMgr     getSessionsForRoom() → 세션 목록
                                       │
9. WebSocketSession        sendMessage(TextMessage(JSON))  ← 모든 멤버
```

## 4. 도메인 이벤트 라이프사이클

```
service.action()                    ChatEventListener
     │ @Transactional                 │
     │ ┌──────────────────┐           │
     │ │ DB write         │           │
     │ │ DB write         │           │
     │ │ publishEvent(X)  │ ────────► │ (큐잉, AFTER_COMMIT까지 보류)
     │ │ ...              │           │
     │ └──────────────────┘           │
     │ COMMIT                         │
     │ ─────────────────────────────► │ @Async 비동기 디스패치
     │                                │ broadcastToRoom(...)
     │ return                         │
```

이벤트 종류:
- `MessageSent` — 메시지 저장 후 → `new_message` WS 브로드캐스트
- `MessageRead` — 읽음 표시 후 → `read_update`
- `RoomExpiring` — 만료 10분 전 → `room_expiring` (스케줄러 발행, `@EventListener` 비트랜잭션)
- `RoomExpired` — 방 정리 후 → `room_expired`
- `MessageExpired` — 메시지 hard delete 후 → `message_expired`
- `UserConnected` / `UserDisconnected` — 향후 presence 기능용 (현재 listener 없음)

## 5. 인증 플로우 (계획 — 트랙 B 완료 후)

### 5.1 일반 로그인
```
POST /auth/signup {username, password, nickname, email?}
  ↓ validate, BCrypt hash
  ↓ User.save(passwordHash)
  ↓ JWT access (15m) + refresh (14d, DB)
  → {accessToken, refreshToken}

POST /auth/login {username, password}
  ↓ User.findByUsername
  ↓ status == ACTIVE 확인
  ↓ BCrypt.matches
  → {accessToken, refreshToken}

POST /auth/withdraw  (auth)
  ↓ user.anonymize()
  ↓ UserProvider.deleteAllByUserId
  ↓ RefreshToken.deleteAllByUserId
  → 200
```

### 5.2 OAuth + 닉네임 후설정 (Deferred User Creation)
```
GET /auth/google                 → 302 to https://accounts.google.com/...
  Google → GET /auth/callback?code=xxx&state=google
    ↓ exchange code for access_token
    ↓ fetch profile (id, email, name, picture)
    ↓ AuthService.oauthCallback(profile):
       (a) UserProvider.findByProviderAndProviderId(GOOGLE, id)
           → 있으면 LoggedIn(tokens)
       (b) UserProvider.findByEmail(email)
           → 있으면 LinkingRequired(linkToken, existingNickname, newProvider)
       (c) 없으면 SignupRequired(signupToken, suggestedNickname)
    ↓ 302 to FRONTEND_URL/auth/{success|oauth-link|oauth-complete}#...
  Frontend strips hash, calls API:
    /auth/oauth/complete-signup {signupToken, nickname}
       → Create User + UserProvider, return tokens
    /auth/link-provider {linkToken, confirm: true|false}
       → Add provider OR fall through to signup-required
```

## 6. WebSocket 세션 관리

`WebSocketSessionManager` (in-memory, ConcurrentHashMap):
- `userSessions: userId → Set<WebSocketSession>` (멀티 디바이스 지원, 최대 3)
- `sessionToUser: sessionId → userId`
- `userRooms: userId → Set<roomId>` (브로드캐스트 라우팅용)
- `lastConnectionTime: userId → ms` (재연결 rate limit, 2초)

연결 시: JWT 검증 → 등록 → `chatRoomMemberRepository.findAllByUserId()`로 모든 방 자동 join.

## 7. 스케줄러

`@EnableScheduling` (GeekChatServerApplication.kt).

| 스케줄러 | 주기 | 동작 |
|---|---|---|
| `MessageExpirationScheduler` | 60초 | 만료 메시지 hard delete + `MessageExpired` event |
| `RoomExpirationScheduler` | 600초 | (1) 만료 10분 전 방 → `RoomExpiring` (warnedRoomIds로 중복 방지), (2) 만료 방 → 메시지/멤버 정리 → `RoomExpired` |

스케줄러는 `@Transactional` 적용. 단일 인스턴스 가정 (멀티 인스턴스 시 분산 락 필요 — 향후).

## 8. 에러 처리: Either<ChatError, T>

```kotlin
sealed class ChatError {
    abstract val message: String
    // 카테고리별 sealed sub-types: Auth, User, Room, Message, WebSocket, InviteLink, Generic
}

sealed class Either<out L, out R> {
    data class Left<L>(val value: L) : Either<L, Nothing>()
    data class Right<R>(val value: R) : Either<Nothing, R>()
    fun <T> fold(onLeft: (L) -> T, onRight: (R) -> T): T = ...
    fun <T> map(f: (R) -> T): Either<L, T> = ...
    fun <T> flatMap(f: (R) -> Either<L, T>): Either<L, T> = ...
}
```

서비스 → 컨트롤러 변환:
```kotlin
@PostMapping("/api/x")
fun handle(...): ResponseEntity<*> =
    service.doX(...).fold(
        onLeft = { it.toResponseEntity() },  // ChatErrorMapping.kt
        onRight = { ResponseEntity.ok(...) },
    )
```

## 9. 트랜잭션 경계

| 메서드 | 트랜잭션 | 비고 |
|---|---|---|
| `AuthService.devLogin` | `@Transactional` | User + UserProvider + RefreshToken 생성 |
| `AuthService.refreshToken` | `@Transactional` | rotation: 삭제 + 발급 |
| `MessageService.sendMessage` | `@Transactional` | 메시지 + 방 lastMessageAt + 이벤트 |
| `ChatRoomService.createRoom` | `@Transactional` | 방 + 멤버 + 이벤트 |
| `InviteLinkService.joinByInviteCode` | `@Transactional` | 멤버 추가 + 사용 횟수 증가 |
| Scheduler 메서드 | `@Transactional` | AFTER_COMMIT 이벤트 위해 필수 |

조회 전용 메서드는 `@Transactional(readOnly = true)`로 명시 권장 (현재 일부만 적용).

## 10. 보안 필터 체인

```
HTTP request
   │
   ▼
CorsFilter  (allowedOriginPatterns=[FRONTEND_URL, *.vercel.app, ...])
   │
   ▼
JwtAuthenticationFilter  ← Bearer 토큰 → SecurityContext에 userId 세팅
   │
   ▼
UsernamePasswordAuthenticationFilter (Spring 기본, 미사용)
   │
   ▼
Controller @AuthenticationPrincipal userId: String
```

`SecurityConfig.kt`:
- `/auth/me`, `/auth/withdraw`, `/api/**` → 인증 필요
- `/auth/**` (그 외), `/health`, `/ws/**` → permitAll
- 인증 실패 시 `HttpStatusEntryPoint(401)` (403이 아님)
