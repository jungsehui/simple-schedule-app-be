# PRD — M2 마일스톤 (다음 단계 기능 후보)

이 문서는 GeekChat v2의 **두 번째 마일스톤(M2)** 후보 기능을 정리한 raw material이다.
정식 사양 결정 전, 어떤 기능을 어떤 우선순위로 넣을지 빠르게 합의하기 위한 자료.

작성일: 2026-05-08
대상: 다음 세션 / 제품 기획 회의

---

## 0. 포지셔닝 가설

GeekChat v2 = **"프라이버시 메신저 + 개발자/얼리어답터 친화"**.

- 카카오톡/라인 100% 추격 ❌
- "메신저로서 신뢰감을 주는 최소 기능 + 우리만의 차별화 1-2개"가 목표
- 비-목표: 음성/영상 통화, E2EE, 오픈채팅 (각각 별도 마일스톤)

---

## 1. 카카오톡/라인 기본 기능 인벤토리 + GeekChat v2 갭

(O = 완전 지원, △ = 부분/제한적, X = 없음)

| 카테고리 | 기능 | 카카오 | 라인 | v2 현재 |
|---|---|---|---|---|
| **인증** | ID/PW 가입 | △ (전화 우선) | △ (전화 우선) | **O** |
| 인증 | 전화번호 인증 (SMS) | O | O | X |
| 인증 | 이메일 인증/비밀번호 재설정 | O | O | X (email 컬럼만 존재) |
| 인증 | 소셜 로그인 | △ | O | **O** (Google/Naver) |
| 인증 | 멀티 디바이스 동시 접속 | O | O | △ (WS 다중 세션 가능, 정책 미명시) |
| 인증 | 2FA / 디바이스 검증 | O | O | X |
| **프로필** | 사진 | O | O | △ (URL 컬럼, 업로드 X) |
| 프로필 | 상태 메시지 / Bio | O | O | X |
| 프로필 | 닉네임 변경 | O | O | △ (username만) |
| **친구** | 연락처 동기화 | O | O | X |
| 친구 | 친구 추가/삭제 | O | O | X (검색만) |
| 친구 | 친구 차단 | O | O | X |
| 친구 | QR / ID 추가 | O | O | △ (username 검색만) |
| 친구 | 즐겨찾기 / 그룹 | O | O | X |
| **채팅 코어** | 1:1 / 그룹 | O / O(~1500) | O / O(~500) | **O / O(100)** |
| 채팅 코어 | 오픈채팅 (공개방) | O | O (Square) | X |
| 채팅 코어 | 채팅방 검색 / 고정 / 즐겨찾기 | O | O | X |
| 채팅 코어 | 알림 끄기 (mute) | O | O | X |
| 채팅 코어 | 나가기 / 강퇴 / 방장 권한 | O | O | X (멤버십만 존재) |
| 채팅 코어 | 메시지 검색 | O | O | X |
| **메시지 액션** | 답장 (reply) | O | O | X |
| 메시지 액션 | @mention | O | O | X |
| 메시지 액션 | 메시지 전달 (forward) | O | O | X |
| 메시지 액션 | 메시지 삭제 (전체/나만) | O | O (24h) | △ (TTL hard delete만) |
| 메시지 액션 | 메시지 수정 | △ | O (24h) | X |
| 메시지 액션 | 이모지 리액션 | △ | O | X |
| 메시지 액션 | 메시지 신고 | O | O | X |
| **미디어** | 이미지/동영상 | O | O | X |
| 미디어 | 파일/문서 | O | O | X |
| 미디어 | 음성 메시지 | O | O | X |
| 미디어 | 스티커 / GIF | O | O | X |
| 미디어 | 링크 미리보기 (OG) | O | O | X |
| 미디어 | 위치 / 연락처 / 일정 공유 | O | O | X |
| **통화** | 음성 / 영상 / 그룹콜 | O | O | X |
| 통화 | 화면 공유 | △ | O | X |
| **알림** | 푸시 (FCM/APNs) | O | O | X |
| 알림 | 알림 미리보기 토글 | O | O | X |
| 알림 | 키워드 알림 | △ | O | X |
| **백업/싱크** | 멀티 디바이스 메시지 싱크 | O | O | △ (재연결 시 fetch) |
| 백업/싱크 | 클라우드 백업 / 내보내기 | O | O | X |
| **보안/관리** | E2EE | △ (비밀채팅) | O (Letter Sealing) | X |
| 보안/관리 | 차단 사용자 관리 | O | O | X |
| 보안/관리 | 신고 / 운영자 패널 | O | O | X |
| **부가** | 캘린더/투두/투표 | O | O | X |
| 부가 | 봇 / Webhook | △ | O | X |

**v2가 가지고 있는 메신저 기본 기능**: 가입(일반+소셜), 1:1 + 그룹 채팅, 멤버십, WebSocket 실시간, 멱등성, 읽음 표시, typing, 검색.

**v2만의 차별화 (이미 구현)**: 임시 방 (TTL), 자동삭제 메시지 (Disappearing), 초대 링크 (TTL+사용 횟수).

---

## 2. M2 마일스톤 권장 (P0 + P1)

가정: M1(현재)이 "텍스트 채팅 + 프라이버시 코어 + 인증"의 베타 출시. M2는 **"메신저로 매일 쓸 수 있게"** + **"차별화 1개 spike"**.

### P0 — 반드시 (M2 정의 자체)

| # | 기능 | 한 줄 정의 | 의존성/비고 |
|---|---|---|---|
| 1 | **푸시 알림 (Web Push, FCM)** | 백그라운드/오프라인 시 새 메시지/멘션/만료 임박 알림 | `adapter/out/notification/` 빈 폴더 이미 존재. 새 port `PushNotifier` 추가 |
| 2 | **메시지 답장 (reply)** | message에 `replyToMessageId` + WS payload 확장 | Message 도메인 + 인덱스 추가 |
| 3 | **사용자 메시지 삭제** (전체/나만) | 사용자가 자기 메시지 hard/soft delete | TTL hard delete 로직과 통합 가능. 권한: 본인만 |
| 4 | **메시지 수정** (15분 내) | edited_at 컬럼 + 새 WS event `message_edited` | reply와 같이 도입 권장 |
| 5 | **채팅방 나가기 / 강퇴 / 방장 권한** | ChatRoomMember.role + 액션 API | DIRECT 방 자동 정리 정책 결정 필요 |
| 6 | **알림 끄기 (mute room)** | ChatRoomMember.muted 플래그 | 푸시(#1)와 동시 도입 |
| 7 | **친구 / 차단 모델** | follow 단방향 + block 테이블 | 검색/방생성 가드 추가 |

### P1 — 강력 추천 (자원 허용 시)

| # | 기능 | 정의 | 비고 |
|---|---|---|---|
| 8 | **이미지 전송** | S3/R2 presigned URL → MessageType.IMAGE | 미디어 인프라 첫 설치, 비용/모더레이션 정책 동시 |
| 9 | **이모지 리액션** | message_reaction 테이블 + WS event | reply(P0)와 합치면 채팅 UX 도약 |
| 10 | **@mention + 멘션 알림** | 메시지 파싱 + ChatEvent.MentionAdded → 푸시 트리거 | 차단 가드 적용 |
| 11 | **링크 미리보기 (OG)** | 클라이언트 URL 감지 → 백엔드 메타 fetch + 캐시 | 별도 서비스 분리 권장 |
| 12 | **차별화 spike: Burn-on-Read** 또는 **Markdown/Code Block** | 아래 §3 참조 | 마케팅 메시지에 한 줄 |

### 의도적 제외 (M2 아님)

- 음성/영상 통화 (M3+, WebRTC 별도)
- E2EE (M3+, 멀티 디바이스/검색과 충돌)
- 오픈채팅 (모더레이션 인프라 선행 필요)
- 메시지 검색 (P2, FULLTEXT 한국어 토크나이저 이슈)

---

## 3. 우리만의 차별화 후보 (X 프라이버시 외)

| # | 기능 | Why (왜 다른가) | Effort |
|---|---|---|---|
| C1 | **Burn-on-Read** | 한 번 읽으면 삭제. 시간 기반 TTL과 보완. 시그널/스냅챗 스타일. 자동삭제 인프라 재사용 | small |
| C2 | **Read-receipt 끄기 (수신자 토글)** | 카카오 불가, 라인 토글. 프라이버시 포지셔닝과 직결 | small |
| C3 | **Anonymous Room** | 방 안에서 일시 익명 모드. 토론/익명 피드백 방. ChatRoomMember.displayMode 추가 | medium |
| C4 | **Markdown / Code Block / Syntax Highlight** | 개발자 페르소나. Discord 부분 지원. MessageType 확장 | small |
| C5 | **메시지 단위 만료 시간 개별 설정** | 방 기본 + 메시지별 override | small |
| C6 | **Invite Link 추가 가드** | 1회용 + 관리자 승인 + 가입자만 | medium |
| C7 | Ephemeral Voice Note (자동삭제 음성) | 시그널 일부 지원. 한국 거의 없음 | large (코덱+스토리지) |
| C8 | Per-message E2EE (일부만) | "이 메시지만 암호화" 토글. 점진적 도입 가능 | large |
| C9 | **Self-Destruct Account Schedule** | 30일 미접속 시 자동 익명화 옵션. 익명화 로직 재사용 | small |
| C10 | **Webhook In/Out** | 슬랙 incoming webhook 미니 버전. 개발자 강력 | medium |
| C11 | Federated Identity Reveal | 익명방 → 신원공개 양방 합의 핸드셰이크 | medium |
| C12 | **Topic Threads** | 그룹방 컨텍스트 분리. message.parentId 추가만으로 시작 | medium |

**M2 차별화 픽 (1-2개)**: **C1 (Burn-on-Read), C4 (Markdown/Code), C2 (Read-receipt 끄기)** — 모두 small + "프라이버시 + 개발자" 일관 메시지.

---

## 4. M2 실행 시 기술 부채/제약

1. **MessageType enum이 `TEXT, SYSTEM` 둘 뿐** — IMAGE/FILE/AUDIO/REPLY/EDITED 추가 시 마이그레이션 필요.
2. **ChatEvent sealed class는 확장에 강함** — `MessageEdited`, `MessageDeleted`, `ReactionAdded`, `MentionAdded` 추가 자연스러움.
3. **`adapter/out/notification/` 폴더 비어있음** — 푸시 어댑터 자리 이미 마련됨. M2 P0 #1 작업 시 신규 port 추가가 헥사고날 규칙에 정확히 부합.
4. **MessageService에 `clientMessageId` 멱등성 3중 방어** — reply/edit 추가 시 동일 패턴 유지.
5. **회원 탈퇴 익명화 정책** — 차단/친구 모델 도입 시 탈퇴 사용자 노출 처리 일관 필요.
6. **TTL hard delete 스케줄러는 분 단위** — Burn-on-Read(C1) 도입 시 즉시 삭제 경로 별개 필요.
7. **친구 클라우드 (4 vCPU / 8GB)** — 이미지 업로드(P1 #8)는 메모리 우회 (presigned URL 직접 업로드) 필수. 서버 경유 multipart 금지.
8. **WebSocket 단일 인스턴스 가정** — 친구/차단 매트릭스가 broadcast 필터링에 들어가면 멀티 인스턴스 확장 시 부담 증가. M2까지는 단일 인스턴스 OK.

---

## 5. 마케팅 메시지 후보 (M2 출시용)

- 슬로건: **"사라지는 채팅, 이제 매일 써도 되는 메신저"**
- 핵심 메시지: 메신저 기본 (답장/멘션/리액션/푸시) + 우리답게 (Burn-on-Read 또는 Markdown).
- 비-목표 명시: 통화 ❌ / E2EE ❌ / 오픈채팅 ❌ — 정직한 한계 표시.

---

## 6. 다음 단계

1. 이 문서를 기반으로 정식 PRD 작성 (P0 7개 + P1 5개 + 차별화 1개 픽)
2. 각 P0 기능마다 사양서 (스키마 변경, API, WS, UX) — 별도 문서
3. 우선순위 확정 + 스프린트 분할 (2-week sprint × 3-4)
4. 백엔드 / 프론트엔드 동시 진행 (백엔드 대부분 already-paved)
