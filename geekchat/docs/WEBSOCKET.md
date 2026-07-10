# WebSocket Events

GeekChat v2는 STOMP를 사용하지 않고 **Raw WebSocket + JSON envelope** 방식이다.

## 1. 연결

```
Client: new WebSocket(`wss://api.<domain>/ws?token=<JWT_ACCESS_TOKEN>`)
```

- **URL**: `/ws`
- **인증**: query string `?token=<accessToken>`
- **Subprotocol**: 없음

연결 시 서버가:
1. JWT 검증 (실패 → `error` 메시지 발송 후 `POLICY_VIOLATION`으로 close)
2. Rate limit (재연결 2초 미만 → `RATE_LIMITED`로 거부)
3. Max 3 connections per user 검증
4. 사용자의 모든 방에 자동 join

## 2. 메시지 envelope 형식

모든 메시지는 다음 JSON 형식:
```json
{ "type": "<event-name>", "data": { ... } }
```

## 3. Inbound 이벤트 (Client → Server)

### 3.1 send_message
```json
{
  "type": "send_message",
  "data": {
    "roomId": "uuid",
    "content": "Hello!",
    "clientMessageId": "uuid",
    "ttlSeconds": 0,
    "replyToMessageId": null,
    "burnAfterRead": false
  }
}
```
- `clientMessageId`: 클라이언트 생성 UUID (멱등성 키, UNIQUE 제약)
- `ttlSeconds`: 0(영구) / 30 / 300 / 3600 / 86400. 메시지가 자동 삭제되는 시간(초)

응답:
- 성공 시 `message_ack` (발신자에게만)
- 실패 시 `error`
- 모든 멤버에게 `new_message` 브로드캐스트 (트랜잭션 커밋 후 비동기)

### 3.2 typing_start
```json
{ "type": "typing_start", "data": { "roomId": "uuid" } }
```
DB 저장 없음. 같은 방의 다른 멤버에게 `typing_indicator` 즉시 브로드캐스트.

### 3.3 mark_read
```json
{ "type": "mark_read", "data": { "roomId": "uuid", "lastReadMessageId": "uuid" } }
```
ChatRoomMember.lastReadAt 갱신 (forward-only). 모든 멤버에게 `read_update` 브로드캐스트.

## 4. Outbound 이벤트 (Server → Client)

### 4.1 message_ack
```json
{
  "type": "message_ack",
  "data": { "clientMessageId": "uuid", "serverId": "uuid" }
}
```
발신자에게만. `serverId`는 서버가 부여한 메시지 ID.

### 4.2 new_message
```json
{
  "type": "new_message",
  "data": {
    "id": "uuid", "roomId": "uuid", "senderId": "uuid",
    "content": "Hello!", "type": "TEXT",
    "createdAt": "2026-04-15T...",
    "expiresAt": null,
    "replyToMessageId": null,
    "burnAfterRead": false
  }
}
```
방 모든 멤버에게 (발신자 포함). 트랜잭션 AFTER_COMMIT 후 비동기.
- `replyToMessageId` (M2 P0): null이 아니면 해당 메시지에 대한 답장. 클라이언트는 인용 UI 표시.
- `burnAfterRead` (M2 spike): true면 발신자 외의 누군가가 mark_read하는 순간 hard delete + `message_burned` 브로드캐스트.

### 4.3 typing_indicator
```json
{ "type": "typing_indicator", "data": { "roomId": "uuid", "userId": "uuid" } }
```
방의 다른 멤버에게 (발신자 제외).

### 4.4 read_update
```json
{ "type": "read_update", "data": { "roomId": "uuid", "userId": "uuid", "lastReadAt": "..." } }
```
방 모든 멤버에게.

### 4.5 room_expiring
```json
{ "type": "room_expiring", "data": { "roomId": "uuid", "roomName": "...", "expiresAt": "..." } }
```
만료 10분 전 1회 발송. (RoomExpirationScheduler가 발행, 중복 방지)

### 4.6 room_expired
```json
{ "type": "room_expired", "data": { "roomId": "uuid", "roomName": "..." } }
```
방이 정리된 직후. 클라이언트는 방 목록에서 제거 처리.

### 4.7 message_expired
```json
{ "type": "message_expired", "data": { "roomId": "uuid", "messageIds": ["uuid", ...] } }
```
1분마다 hard delete된 메시지 ID 배치. 클라이언트는 해당 메시지 UI 제거.

### 4.8 message_burned (M2 spike — Burn-on-Read)
```json
{ "type": "message_burned", "data": { "roomId": "uuid", "messageId": "uuid" } }
```
`burnAfterRead=true` 메시지를 발신자가 아닌 멤버가 read한 직후 발행. 해당 메시지는 DB에서 hard delete됨.
클라이언트는 받자마자 UI에서 메시지 제거. 송신자에게도 동일하게 브로드캐스트되어 양쪽 모두에서 사라진다.

### 4.9 error
```json
{ "type": "error", "data": { "code": "TOKEN_EXPIRED", "message": "Token expired" } }
```

에러 코드:
| code | 의미 |
|---|---|
| `NO_TOKEN` | 토큰 없이 연결 시도 |
| `TOKEN_EXPIRED` | JWT 만료 |
| `INVALID_TOKEN` | JWT 변조/잘못됨 |
| `RATE_LIMITED` | 재연결 너무 빠름 (2초 미만) |
| `MAX_CONNECTIONS` | 동시 연결 4개 시도 (3 초과) |
| `AUTH_ERROR` | 기타 인증 실패 |
| `NOT_AUTHENTICATED` | 메시지 처리 시 인증 컨텍스트 없음 |
| `INVALID_FORMAT` | JSON 파싱 실패 |
| `UNKNOWN_EVENT` | 모르는 type 값 |
| `INVALID_DATA` | 필수 필드 누락 |
| `SEND_FAILED` | sendMessage 비즈니스 로직 실패 |

## 5. 연결 종료 코드

| Close Code | 사유 |
|---|---|
| `1000` (NORMAL) | 클라이언트 정상 종료 |
| `1008` (POLICY_VIOLATION) | 인증/Rate Limit/Max Connections |
| `1011` (SERVER_ERROR) | 서버 내부 오류 |

## 6. 클라이언트 재연결 전략 (권장)

```javascript
class ChatWebSocketClient {
  connect() {
    this.ws = new WebSocket(this.urlWithToken())
    this.ws.onopen = () => { this.backoff = 0 }
    this.ws.onclose = (e) => {
      if (e.code === 1008) {
        // POLICY_VIOLATION: 재시도 의미 없음. 토큰 refresh 후 시도
        this.refreshTokenAndReconnect()
        return
      }
      // 지수 백오프, max 30s
      this.backoff = Math.min((this.backoff || 1000) * 2, 30000)
      setTimeout(() => this.connect(), this.backoff)
    }
    this.ws.onmessage = (e) => this.handle(JSON.parse(e.data))
  }
}
```

- 토큰 만료 시: REST `/auth/refresh` → 새 token으로 재연결
- POLICY_VIOLATION 즉시 재시도 금지 (rate limit 더 악화됨)
- 재연결 직후 cursor 기반 메시지 폴백 fetch (`GET /api/rooms/:id/messages?direction=forward&cursor=lastSeen`)

## 7. 구현 위치

- 핸들러: `adapter/in/websocket/ChatWebSocketHandler.kt`
- 세션 관리: `adapter/in/websocket/WebSocketSessionManager.kt`
- 브로드캐스트 어댑터: `adapter/in/websocket/WebSocketBroadcasterAdapter.kt`
- 도메인 이벤트 → WS 변환: `adapter/in/websocket/ChatEventListener.kt`
- 설정: `infrastructure/config/WebSocketConfig.kt` (path `/ws` 등록 + CORS)
