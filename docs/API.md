# API Reference

이 문서는 모든 REST 엔드포인트의 요청/응답 계약을 정의한다. WebSocket은 `WEBSOCKET.md` 참조.

## Conventions

- **Base URL**: `https://api.<domain>` (production), `http://localhost:8080` (local)
- **Auth**: `Authorization: Bearer <accessToken>`
- **Content-Type**: `application/json`
- **Error 응답**:
  ```json
  { "statusCode": 401, "message": "...", "error": "Unauthorized" }
  ```
- **타임스탬프**: ISO-8601 (`2026-04-15T00:00:00Z`)

## 1. Auth

### 1.1 [LOCAL] POST /auth/signup
일반 ID/PW 회원가입.
- **Auth**: 없음
- **Request**:
  ```json
  { "username": "alice", "password": "hunter2x", "nickname": "Alice", "email": "alice@example.com" }
  ```
  - `username`: `^[a-z0-9_]{3,20}$`
  - `password`: 최소 8자, 영문 + 숫자 1개 이상
  - `nickname`: 1-20자
  - `email`: 선택. 비밀번호 찾기용. 중복 불가
- **Response 200**:
  ```json
  { "accessToken": "eyJ...", "refreshToken": "uuid-..." }
  ```
- **Errors**: 400 (WeakPassword, InvalidUsername), 409 (UsernameTaken, EmailAlreadyInUse)

### 1.2 [LOCAL] POST /auth/login
- **Auth**: 없음
- **Request**: `{ "username": "alice", "password": "hunter2x" }`
- **Response 200**: `{ "accessToken", "refreshToken" }`
- **Errors**: 401 (InvalidCredentials), 403 (AccountWithdrawn)

### 1.3 [AUTH] POST /auth/withdraw
회원 탈퇴 (익명화).
- **Auth**: 필요
- **Request**: 빈 바디
- **Response 200**: `{ "success": true }`
- **효과**: User.status=WITHDRAWN, nickname=`deleted_user_<8hex>`, email/profileImageUrl null, 모든 UserProvider/RefreshToken hard delete. 메시지/방은 그대로.

### 1.4 [OAUTH] GET /auth/google / GET /auth/naver
OAuth 시작.
- **Auth**: 없음
- **Response**: 302 → provider authorize URL

### 1.5 [OAUTH] GET /auth/callback
OAuth 공통 콜백 (provider 자동 redirect).
- **Auth**: 없음
- **Query**: `code`, `state` (`google`|`naver`), `error?`
- **Response**: 302 → `${FRONTEND_URL}/auth/{success|oauth-link|oauth-complete}#...`
  - `LoggedIn` → `/auth/success#access_token=...&refresh_token=...`
  - `LinkingRequired` → `/auth/oauth-link#link_token=...&existing_nickname=...&new_provider=...`
  - `SignupRequired` → `/auth/oauth-complete#signup_token=...&suggested_nickname=...`
  - error → `/auth/error?error=oauth_failed`

### 1.6 [OAUTH] POST /auth/link-provider
계정 링크 결정.
- **Auth**: 없음 (linkToken JWT가 인증 역할)
- **Request**: `{ "linkToken": "eyJ...", "confirm": true|false }`
  - `confirm=true`: 기존 계정에 새 provider 연결
  - `confirm=false`: 새 계정 생성 흐름으로 (signupToken 발급)
- **Response 200**:
  - confirm=true: `{ "accessToken", "refreshToken" }`
  - confirm=false: `{ "signupToken", "suggestedNickname" }` (302 대신 JSON)
- **Errors**: 401 (InvalidLinkToken, ProviderAlreadyLinked)

### 1.7 [OAUTH] POST /auth/oauth/complete-signup
OAuth 후 닉네임 입력 완료.
- **Auth**: 없음 (signupToken JWT가 인증 역할)
- **Request**: `{ "signupToken": "eyJ...", "nickname": "Alice" }`
- **Response 200**: `{ "accessToken", "refreshToken" }`
- **Errors**: 400 (NicknameRequired, InvalidUsername), 401 (InvalidLinkToken)

### 1.8 [TOKEN] POST /auth/refresh
- **Auth**: 없음
- **Request**: `{ "refreshToken": "uuid-..." }`
- **Response 200**: `{ "accessToken", "refreshToken" }` (rotation: 새 refresh도 발급)
- **Errors**: 401 (InvalidRefreshToken, RefreshTokenExpired)

### 1.9 [TOKEN] POST /auth/logout
- **Auth**: 없음 (idempotent)
- **Request**: `{ "refreshToken": "uuid-..." }`
- **Response 200**: `{ "success": true }`

### 1.10 [USER] GET /auth/me
- **Auth**: 필요
- **Response 200**:
  ```json
  { "id": "uuid", "nickname": "Alice", "username": "alice", "profileImageUrl": null }
  ```
- **Errors**: 401

### 1.11 [DEV] GET /auth/dev-login?name=X
- **Profile**: `dev` 활성화 시만
- **Auth**: 없음
- **Response 200**: `{ "accessToken", "refreshToken", "message": "Logged in as X" }`
- **Errors**: 404 (prod), 400 (name 누락)

## 2. Users

### 2.1 [USER] GET /api/users/search?q=X
- **Auth**: 필요
- **Query**: `q` (1자 이상)
  - `@username` 형식: 정확 매칭
  - 그 외: 닉네임 ILIKE 부분 매칭
- **Response 200**:
  ```json
  [{ "id": "uuid", "nickname": "...", "username": "...", "profileImageUrl": null }, ...]
  ```
  최대 10개. 본인 제외.

### 2.2 [USER] PATCH /api/users/me/username
- **Auth**: 필요
- **Request**: `{ "username": "new_username" }`
- **Response 200**: `{ "username": "new_username" }`
- **Errors**: 400 (InvalidUsername), 409 (UsernameAlreadyTaken)

## 3. Rooms

### 3.1 [ROOM] GET /api/rooms
- **Auth**: 필요
- **Response 200**:
  ```json
  [
    {
      "id": "uuid",
      "type": "DIRECT",
      "name": null,
      "lastMessageAt": "2026-04-15T...",
      "expiresAt": null,
      "members": [
        {"userId": "uuid", "nickname": "...", "profileImageUrl": null}
      ]
    }
  ]
  ```
  `lastMessageAt` DESC 정렬.
  > HATEOAS(Phase 7): 목록은 plain 최상위 배열 유지 — `_embedded`/per-element `_links` 없음(프론트 배열 계약 보존).

### 3.2 [ROOM] POST /api/rooms
- **Auth**: 필요
- **Request**:
  ```json
  { "memberIds": ["uuid"], "name": "Optional Group Name", "ttlHours": 24 }
  ```
  - `memberIds.size==1 && name==null` → DIRECT (기존 있으면 반환)
  - 그 외 → GROUP
  - `ttlHours`: null(영구) / 24 / 168 / 720 (GROUP만 적용)
- **Response 200**:
  ```json
  {
    "id": "uuid", "type": "GROUP", "name": "Project Team",
    "expiresAt": "2026-04-16T...",
    "members": [{"userId": "uuid", "nickname": "..."}]
  }
  ```
  > HATEOAS(Phase 7, additive): 위 필드에 더해 `_links`가 추가된다 — `self`(`/api/rooms/{id}`), `mute`, `invite-link`. 기존 필드는 그대로. `POST /api/invite/{code}/join`(4.2) 응답도 동일하게 래핑.
- **Errors**: 404 (UserNotFound), 400 (RoomFull)

### 3.3 [MESSAGE] GET /api/rooms/{id}/messages?cursor=X&limit=50&direction=backward
- **Auth**: 필요 (방 멤버만)
- **Query**:
  - `cursor`: ISO-8601 (생략 시 최신부터)
  - `limit`: 1-100, 기본 50
  - `direction`: `forward` | `backward` (기본)
- **Response 200**:
  ```json
  [
    {
      "id": "uuid", "senderId": "uuid", "senderNickname": "Alice",
      "content": "Hello!", "type": "TEXT",
      "createdAt": "2026-04-15T...",
      "expiresAt": null,
      "replyToMessageId": null,
      "burnAfterRead": false
    }
  ]
  ```
- **Errors**: 403 (NotRoomMember), 404 (RoomNotFound), 400 (Invalid cursor)

### 3.4 [ROOM] PATCH /api/rooms/{id}/mute
방 알림 끄기/켜기 (M2 P0).
- **Auth**: 필요 (방 멤버만)
- **Request**: `{ "muted": true }`
- **Response 200**: `{ "muted": true }`
- **Errors**: 403 (NotRoomMember)
- 효과: 사용자별 `ChatRoomMember.muted` 토글. 푸시 알림 라우팅 시 사용 (M2 P1).

## 4. Invite Links

### 4.1 [INVITE] POST /api/rooms/{roomId}/invite-link
- **Auth**: 필요 (방 멤버만)
- **Request**: `{ "ttlHours": 24, "maxUses": null }`
- **Response 200**:
  ```json
  { "code": "AbC12dEf", "roomId": "uuid", "expiresAt": "...", "maxUses": null, "currentUses": 0 }
  ```
- **Errors**: 404 (RoomNotFound), 403 (NotRoomMember)

### 4.2 [INVITE] POST /api/invite/{code}/join
- **Auth**: 필요
- **Response 200**: 방 객체 (POST /api/rooms 응답과 동일)
- **Errors**: 404 (InviteLinkNotFound), 410 (InviteLinkExpired, InviteLinkMaxUsesReached), 409 (AlreadyRoomMember), 400 (RoomFull)

## 5. AI (관리자 전용)

### 5.1 [ADMIN] POST /api/ai/chat
- **Auth**: 필요 + **ADMIN role** (`@PreAuthorize("hasRole('ADMIN')")`). 일반 사용자 403, 익명 401
- **Request**: `{ "message": "이 방 요약해줘" }`
- **Response 200**: `{ "reply": "..." }`
- provider는 서버 config `spring.ai.model.chat`(`AI_CHAT_PROVIDER`, 기본 `anthropic`)로 선택 — `anthropic` | `openai` | `ollama`. 코드 변경 없이 교체
- AI provider 키 미설정 시: `reply`에 "AI chat is not configured..." 안내 반환 (앱은 정상 동작, 사람:사람 채팅 무영향)

## 6. Health

### 6.1 GET /health
- **Auth**: 없음
- **Response 200**:
  ```json
  { "status": "ok", "db": "connected", "uptime": 123.45, "timestamp": "..." }
  ```
  DB 끊긴 경우 `status: "degraded"`, `db: "disconnected"`.

## 7. HTTP Status 매핑 (ChatError → HTTP)

| ChatError | HTTP |
|---|---|
| TokenNotProvided / TokenExpired / InvalidToken / RefreshTokenRequired / RefreshTokenExpired / InvalidRefreshToken / InvalidLinkToken / ProviderAlreadyLinked / NotAuthenticated / InvalidCredentials | 401 |
| AccountWithdrawn / NotRoomMember | 403 |
| UserNotFound / RoomNotFound / MessageNotFound / InviteLinkNotFound | 404 |
| InviteLinkExpired / InviteLinkMaxUsesReached | 410 |
| UsernameAlreadyTaken / UsernameTaken / DirectRoomAlreadyExists / EmailAlreadyInUse / AlreadyRoomMember | 409 |
| InvalidUsername / WeakPassword / NicknameRequired / MessageTooLong / EmptyMessage / RoomFull / RateLimited / MaxConnectionsExceeded | 400 |
| OAuthExchangeFailed / OAuthProfileMissingId | 502 |
| Internal | 500 |

## 8. curl 예제

```bash
# Signup
curl -X POST :8080/auth/signup -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"hunter2x","nickname":"Alice"}'

# Login
curl -X POST :8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"hunter2x"}'

# Authenticated request
TOKEN="eyJ..."
curl -H "Authorization: Bearer $TOKEN" :8080/auth/me

# Create DIRECT room
curl -X POST :8080/api/rooms -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"memberIds":["other-user-uuid"]}'

# Withdraw
curl -X POST :8080/auth/withdraw -H "Authorization: Bearer $TOKEN"
```
