# HANDOFF — 다음 세션 진입점 (server-v2 canonical)

마지막 갱신: 2026-05-16
이 파일은 **server-v2 repo 루트**의 정식 핸드오프다 (clone 시 함께 따라옴).
워크스페이스 루트(`~/Work/geek-chat/HANDOFF.md`)는 이 파일을 가리키는 포인터일 뿐이다.

> 보안: 이 파일은 PUBLIC repo에 포함된다. 사설 IP / SSH 유저 / VPN 피어 등
> 인프라 자격증명은 **절대 여기에 적지 않는다**. 비공개 채널/로컬 메모 참조.

---

## 1. 프로젝트 상태 한 줄

**GeekChat v2 — 백엔드 M2 P0 2개(Mute, Reply) + 차별화 spike(Burn-on-Read) 구현 완료. fresh clone에서 `./gradlew clean test` → 112 tests 전부 통과 + GitHub `geek-chat/server-v2`(PUBLIC) main push 완료 + 코드↔문서 drift 0. 다음 세션은 잔여 M2 P0 5개 또는 프론트엔드 시작.**

---

## 2. 주요 저장소

| 저장소 | URL | 상태 |
|---|---|---|
| **server-v2** (Kotlin/Spring Boot) | https://github.com/geek-chat/server-v2 | PUBLIC, main `96aa85c` push 완료, clone-build 검증됨 |
| **web-v2** (Next.js, 핸드오프만) | https://github.com/geek-chat/web-v2 | 핸드오프 문서만, 코드 X |
| v1 server (NestJS) | github.com/geek-chat/server | **수정 금지**, 참조 전용 |
| v1 web (Expo) | github.com/geek-chat/web | **수정 금지**, 참조 전용 |

---

## 3. 마지막 commit (server-v2, origin/main)

```
96aa85c chore(.claude): refresh-context skill + stale-docs Stop hook
fad7e5a docs: sync API/WEBSOCKET/DATABASE for M2 features
28e28f1 feat: Reply (M2 P0) + Burn-on-Read (privacy spike)
e4d94d1 fix(.gitignore): tighten 'out/' to '/out/' and add missing adapter/out tree
346162a docs: add PRD-M2.md (next milestone feature roadmap)
```

`origin/main == HEAD == 96aa85c`. 미푸시 0건, working tree clean.

---

## 4. 완료 항목 (M2 Phase 4)

- [x] **GitHub push** — server-v2(PUBLIC), web-v2(핸드오프 문서)
- [x] **Mute Room (M2 P0)**:
  - `ChatRoomMember.muted: Boolean = false` + `withMuted()`
  - `ChatRoomMemberJpaEntity.muted` 컬럼 + round-trip
  - `ChatRoomService.setRoomMuted(userId, roomId, muted)`
  - `PATCH /api/rooms/{id}/mute` + `MuteRoomRequest` DTO
  - 단위 테스트 2개
- [x] **Reply (M2 P0)**:
  - `Message.replyToMessageId: String? = null`
  - JPA `reply_to_message_id` 컬럼 + `idx_message_reply_to` 인덱스
  - `ChatEvent.MessageSent.replyToMessageId`
  - `MessageService.sendMessage(..., replyToMessageId)` + 같은 방 검증 (cross-room은 동일 에러로 정보 누출 방지)
  - WS handler `replyToMessageId` 추출 + `ChatEventListener.onMessageSent` broadcast
  - `MessageResponse.replyToMessageId`
  - 단위 테스트 3개 (성공 / 없는 타깃 / 다른 방 타깃)
- [x] **Burn-on-Read (차별화 spike)**:
  - `Message.burnAfterRead: Boolean = false`
  - JPA `burn_after_read` 컬럼
  - `ChatEvent.MessageBurned`
  - `MessageService.handleBurnAfterReadOnAck(messageId, readerId)` — 비송신자 read 시 hard delete (송신자 자기 메시지 read는 no-op, 반복 read 멱등)
  - `markAsRead`에서 자동 호출
  - WS handler `burnAfterRead` 추출 + `ChatEventListener.onMessageBurned` (트랜잭셔널 AFTER_COMMIT) → `message_burned` WS event (방 전체 broadcast — sender 포함, UI 일관성)
  - `MessageResponse.burnAfterRead`
  - 단위 테스트 2개
- [x] **RoundTripMappingTest** 확장 (Message.replyToMessageId/burnAfterRead, ChatRoomMember.muted)
- [x] **docs sync 검증**: API.md(3.4 mute + reply/burn 필드), WEBSOCKET.md(4.1~4.9, 4.8 message_burned), DATABASE.md(3개 컬럼+idx) — **drift 0 확인됨**
- [x] **refresh-context 스킬** (`.claude/commands/refresh-context.md`) — HANDOFF 경로 repo 내부로 갱신됨
- [x] **check-stale-docs.sh** Stop hook (advisory, non-blocking)
- [x] **fresh clone 빌드 검증**: `git clone` → `./gradlew clean test` → BUILD SUCCESSFUL, **112 tests** (gitignore `out/`→`/out/` 복구가 clone 관점에서 확정됨)

**테스트 카운트(확정)**: **112** (fresh clone test-results XML 합산)

---

## 5. 미커밋/대기 작업

**없음.** 코드 + 문서 + .claude 모두 commit + push 완료. working tree clean.
다음 세션은 바로 새 기능 착수 가능.

---

## 6. 다음 세션 우선순위 Top 3

### 1순위: 잔여 M2 P0 기능 (택일 또는 분할)

| 기능 | effort | 의존 |
|---|---|---|
| Message Edit | small | `edited_at` 컬럼 + endpoint + WS event |
| Message Delete (own/all) | small-medium | 권한 체크 + TTL hard delete와 통합 결정 |
| Leave/Kick + Role | medium | `ChatRoomMember.role` enum + DIRECT 방 정리 정책 |
| Friends/Block | medium | 신규 도메인 2개 + 검색/방생성 가드 |
| Web Push (FCM) | large | 서비스워커 + FCM 키 + 신규 port |

각 기능 시작 시 `/add-domain-model` 또는 `/add-rest-endpoint` 슬래시 커맨드 활용.
**미구현 watchpoint**: Track B B.5 — `GET /api/rooms` 응답에 사용자별 `muted` 표시 미구현
(현재 PATCH로 설정만 가능). `RoomWithMembers`에 `myMuted` 필드 추가 필요.

### 2순위: 프론트엔드 시작 (Next.js, web-v2)

`geek-chat-web-v2/HANDOFF/GETTING_STARTED.md` 참고 (워크스페이스 로컬). 스캐폴딩:

```bash
cd ~/Work/geek-chat
npx create-next-app@latest geek-chat-web-v2 --typescript --tailwind --app --eslint --src-dir --import-alias "@/*"
cd geek-chat-web-v2 && npm install zustand swr react-hook-form zod @hookform/resolvers sonner lucide-react
```

### 3순위: 클라우드 배포 (server-v2)

도메인 결정 후 `deploy/README.md` 1절 절차로 첫 배포.
- `deploy/nginx.conf`의 `api.geek-chat.example` → 실제 도메인 치환
- 호스트 / SSH / WireGuard 자격증명은 **repo 미포함** — 비공개 채널 또는 워크스페이스 로컬 메모 참조
- `deploy-check` 슬래시 커맨드로 사전 점검

---

## 7. 알려진 문제 / Watchpoint

- **Track B B.5 (RoomListResponse.muted) 미구현**: `GET /api/rooms` 응답에 사용자별 muted 표시 다음 세션. 서비스 레이어가 `RoomWithMembers`에 `myMuted` 추가 필요.
- **markAsRead burn 통합**: 송신자가 자기 burn 메시지 read해도 안 burn (의도). 수신자 read 시 즉시 hard delete + WS broadcast(방 전체). 단위 테스트 2개로 검증.
- **commit 히스토리 메모**: `e4d94d1`에 gitignore-fix + Mute가 함께 들어감 (atomic 경계 깨짐, 코드는 안전). 정리하려면 interactive rebase 필요 — 권장 안 함.
- **git push 실패 시**: origin이 SSH면 → `git remote set-url origin https://github.com/geek-chat/server-v2.git`

---

## 8. 5분 컨텍스트 잡기 순서 (repo 루트 기준)

1. 이 파일 (`HANDOFF.md`)
2. `AGENTS.md` — 프로젝트 개요 + 헥사고날 규칙 + 에이전트 규칙
3. `.claude/RULES.md` — 절대 금지 + 코드 패턴
4. `docs/PRD-M2.md` — 남은 M2 기능 우선순위
5. `docs/API.md` — 현재 엔드포인트 (1.x~5.x)
6. `docs/WEBSOCKET.md` — WS 이벤트 (4.1~4.9)
7. (프론트 작업 시) `~/Work/geek-chat/geek-chat-web-v2/HANDOFF/GETTING_STARTED.md`

---

## 9. 자주 막히는 곳

| 증상 | 원인 / 대처 |
|---|---|
| `PreToolUse:Edit/Write hook` "READ-BEFORE-EDIT" 차단 | 수정할 파일 먼저 Read. 새 세션은 매번 처음 |
| `gradle bootRun` DB 연결 실패 | dev 프로필은 MySQL 컨테이너 필요: `docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=test -e MYSQL_DATABASE=geekchat mysql:8.0` |
| `git push` 실패 (server-v2) | origin SSH면 HTTPS로: `git remote set-url origin https://github.com/geek-chat/server-v2.git` |
| H2 테스트 실패 | `application.yml` test 프로필 `MODE=MySQL` 확인 |
| `domain/`에 JPA import 차단 | hook이 옳음. `adapter/out/persistence/entity/`로 이동 + `toDomain/fromDomain` 매퍼 |
| Stop hook이 `./gradlew test` 매번 실행 | 의도된 동작. 비활성화: `OMC_SKIP_HOOKS=Stop` env |
| `rm -rf` 차단 | check-bash-safety.sh. `mktemp -d`로 우회하거나 명시적 경로 |

---

## 10. 자동화: refresh-context 스킬

세션 종료 직전 또는 컨텍스트 압축 직전:

```
/refresh-context            # 전체 문서 동기화 + HANDOFF 갱신 + commit 후보
/refresh-context --check-only   # drift 검사만 (Stop hook 자동 호출용)
```

- HANDOFF.md 정식 위치: **이 repo 루트** (`geek-chat-server-v2/HANDOFF.md`) — clone 시 따라옴
- 워크스페이스 루트 `~/Work/geek-chat/HANDOFF.md`는 포인터 스텁
- `.claude/hooks/check-stale-docs.sh`가 Stop 훅 등록됨 (advisory): 코드 대비 docs 누락 시 stderr 경고
- **인프라 자격증명은 HANDOFF/docs/코드에 절대 기록 금지** (PUBLIC repo)

---

## 11. 비-기밀 외부 자원 메모

| 항목 | 값 |
|---|---|
| 노출 포트 | 80, 443만 외부 (nginx → app:8080 + WS) |
| MySQL | utf8mb4 / utf8mb4_unicode_ci / 8.0 |
| Vercel 도메인 | `*.vercel.app` 자동 허용 (`FRONTEND_ORIGIN_PATTERNS`) |
| GitHub org | `geek-chat` (server-v2 PUBLIC) |
| OAuth provider | Google + Naver (콜백: `/auth/callback`) |

> 호스트 IP / SSH 유저 / WireGuard 피어 등 인프라 토폴로지는 PUBLIC repo 보안상
> 여기에 기록하지 않는다. `deploy/README.md` + 비공개 채널 참조.
