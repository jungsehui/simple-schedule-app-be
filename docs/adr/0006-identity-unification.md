# ADR-0006: 사용자 정체성 통합 (SSA member + GeekChat users → account)

- 상태: 제안 (초안, 2026-10-02)
- 결정자: jungsehui + Claude
- 관련: ADR-0003 결정 #2(정준 ID), #3(인증 스택), #4(비밀번호), #10(순서)과 Stage 4, 5, ADR-0005(인증 게이트)

이 ADR은 ADR-0003 Stage 5("정체성 통합")의 상세 결정이다. ADR-0003이 이미 정한 것은 승계하고, 그때 비어 있던 결정(겹치는 계정, 탈퇴, 토큰 재사용, 스키마 관리 선결 조건)을 채운다.

## 배경

### 현재 두 개의 신원 체계가 따로 있다

| 항목 | SSA (`ssa` 스키마) | GeekChat (`geekchat` 스키마) |
|---|---|---|
| 사용자 테이블 | `member`(JOINED) + `student` / `tutor` / `parent` | `users` + `user_provider`(OAuth) + `refresh_token` |
| ID | `member_id` BIGINT IDENTITY | `id` varchar(36), 앱이 만든 UUID |
| 비밀번호 | bcrypt cost 10. 옛 무염 SHA-256 해시는 로그인 때 bcrypt로 갱신 | bcrypt cost 12 |
| 토큰 | HS512, Base64URL 키, 클레임 `memberId`, `role`. access 24시간만 있음 | 원문 바이트 키, 클레임 `sub`(UUID), `role`. access 15분 + refresh 14일 회전 |
| 로그아웃, 갱신, 탈퇴 | 없음 | 있음. 탈퇴는 개인정보 익명화 + `status=WITHDRAWN` |
| 사용자 참조 FK | DB FK는 하위 타입 3개뿐. 나머지 9개 컬럼은 코드상 논리 참조 | DB FK 5개 (`message.sender_id`, `chat_room_member.user_id` 등) |

### 운영 데이터 (2026-10-02 오너 조회)

- 활성 사용자: SSA 7명(STUDENT 4, TUTOR 3), GeekChat 7명(ACTIVE 7)
- 두 시스템에서 겹치는 username 3개: `e2e96637`, `e2e35537s`, `test123412341234`
- SSA 안에서 대소문자만 다른 username 중복: 0건
- bcrypt로 바뀌지 않은 SSA 비밀번호: 6건 (탈퇴 행 포함 집계)

### 스키마 관리가 깨져 있다 (선결 문제)

- SSA Flyway `V1__baseline.sql`은 `SELECT 1;`이고, 운영에서 `ddl-auto: update`가 켜져 있다. 그래서 운영 DDL의 정본이 저장소에 없다.
- GeekChat은 DDL 파일이 없고, 처음에 Hibernate가 만든 스키마를 `validate`로 쓴다.
- 이 상태에서 `account` 엔티티를 추가해 배포하면, 운영에서 Hibernate가 테이블을 임의로 만든다.

## 결정

### 1. `account` 테이블을 신원의 정본으로 둔다 (ADR-0003 #2 승계)

- `account.id`는 BIGINT이고 **SSA `member_id`를 그대로 승계**한다. SSA 회원은 `account.id = member.member_id`라서 SSA 쪽 FK와 논리 참조를 하나도 옮기지 않는다.
- GeekChat 전용 사용자는 `max(member_id)`보다 큰 시퀀스에서 새 `account.id`를 받는다. 기존 UUID는 `account.external_uuid`에 보존한다.
- `geekchat.users`에는 `account_id` BIGINT를 추가한다. 메시지와 채팅방 FK(`users.id` 기준)는 그대로 둔다.
- 로그인 자격(username, 비밀번호 해시, 상태)은 `account`로 모은다. 역할별 프로필(학생 학교, 강사 경력, 닉네임 등)은 각 컨텍스트 테이블에 남긴다.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | BIGINT | PK (SSA는 `member_id` 승계) |
| `external_uuid` | varchar(36) | UNIQUE, NULL 허용 (GeekChat 출신만) |
| `username` | varchar(20) | UNIQUE, 소문자 정규화 (기존 규칙 `^(?=.*[a-z])[a-z0-9_]{3,20}$`) |
| `password_hash` | varchar(255) | NULL 허용 (OAuth 전용 계정) |
| `email` | varchar(255) | NULL 허용 |
| `status` | varchar(20) | `ACTIVE` / `WITHDRAWN` |
| `created_at`, `updated_at`, `withdrawn_at` | timestamptz | |

### 2. 겹치는 username은 같은 사람으로 보되, 본인 확인 전까지 연결하지 않는다

- 오너 결정에 따라, 같은 username은 같은 사람의 계정으로 본다.
- 다만 username이 같다고 실제로 같은 사람이라는 증거는 없다. 그래서 연결은 **대기** 상태로 시작한다.
  - 백필 때 SSA 쪽 자격으로 `account`를 만들고, GeekChat 쪽은 `account_link(account_id, external_uuid, status=PENDING)`로 남긴다.
  - 사용자가 **반대쪽 시스템의 비밀번호**(또는 연결된 OAuth 제공자)로 한 번 인증하면 `CONFIRMED`로 바뀌고, 그때 `users.account_id`가 채워진다.
  - 확정 전에는 통합 로그인으로 반대쪽 데이터에 접근할 수 없다.
- 이것이 이번에 새로 마련하는 본인 확인 수단이다. 이메일이나 휴대폰 인증은 이번 범위에 넣지 않는다.

### 3. 탈퇴는 익명화로 통일한다

- `account.status=WITHDRAWN`, `withdrawn_at`을 기록하고 `username`, `email`, `password_hash`를 지운다.
- SSA 프로필의 `name`, `phone_number`는 익명값으로 바꾸고, GeekChat 프로필은 기존 `User.anonymize()` 규칙을 따른다.
- 수강 기록, 강의, 메시지 같은 업무 데이터는 지우지 않는다. 작성자 표시만 익명이 된다.
- 해당 계정의 refresh token은 모두 삭제한다.
- SSA의 `@SQLDelete` 소프트 삭제 경로는 탈퇴에 쓰지 않는다.

### 4. 토큰은 15분 access + 14일 회전 refresh에 재사용 탐지를 더한다 (ADR-0003 #3 승계 + 확장)

- 발급자는 하나이고, 새 단일 시크릿과 HS512를 쓴다.
- 클레임은 `sub`(account id 문자열), `memberId`(SSA 호환용, 같은 값), `role`, `admin`이다.
- access 15분, refresh 14일이다. refresh할 때마다 새 refresh를 발급하고 만료를 다시 14일로 잡는다. 그래서 14일 동안 한 번도 쓰지 않을 때만 다시 로그인한다.
- **재사용 탐지(신규)**: refresh token에 `family_id`를 둔다. 이미 회전된(사용된) refresh가 다시 들어오면 탈취로 보고 그 family 전체를 폐기한다.
- 전환기에는 옛 SSA 토큰(`memberId`, 24시간)과 옛 GeekChat 토큰(UUID `sub`)을 만료될 때까지 함께 받아들인다. 이 기간은 클라이언트 배포 일정에 맞춰 정한다.

### 5. 비밀번호는 ADR-0003 #4를 승계한다

- `DelegatingPasswordEncoder`로 `{sha256}`도 인식하고, 로그인에 성공하면 BCrypt(12)로 다시 저장한다.
- 남은 SHA-256 계정은 Stage 6(ADR-0003)에서 강제로 재설정한다.

### 6. 스키마 관리부터 바로잡는다 (선결 조건)

- 운영 `pg_dump --schema-only -n ssa -n geekchat`으로 Flyway 기준선을 실제 DDL로 다시 만든다.
- 그 뒤 `ddl-auto`를 `validate`로 바꾼다. 이후 모든 스키마 변경은 Flyway 마이그레이션으로만 한다.

## 단계 (각 단계는 독립적으로 배포하고 롤백할 수 있다)

| 단계 | 내용 | 롤백 |
|---|---|---|
| P0 | Flyway 기준선을 실제 DDL로 다시 만들고 `ddl-auto: validate`로 전환 | 설정만 되돌림 |
| P1 | `account`, `account_link`를 만들고 백필한다. `users.account_id`(NULL 허용)를 추가한다. 백필 정합성 쿼리로 검증한다 | 새 테이블과 컬럼만 버림 |
| P2 | 통합 토큰을 발급하고, 옛 토큰과 새 토큰을 함께 받아들인다. refresh `family_id`와 재사용 탐지를 추가한다 | 발급만 옛 방식으로 되돌림 |
| P3 | 통합 인증 기능(가입, 로그인, 로그아웃, 갱신, 탈퇴, 연결 확인)을 연다. Android와 Web은 자동 갱신을 구현한다 | 기능 플래그로 끔 |
| P4 | 옛 토큰 형식을 제거하고 `users.account_id`를 NOT NULL로 바꾼다 | 되돌리기 어려움. 별도 승인 필요 |

## 결과

- 한 사람이 한 계정으로 SSA와 GeekChat을 쓴다.
- SSA에 없던 로그아웃, 토큰 갱신, 탈퇴가 생긴다.
- 운영 스키마의 정본이 저장소(Flyway)로 돌아온다.
- Android와 Web 앱은 refresh 자동 갱신과 연결 확인 화면을 구현해야 한다.

## 검토한 대안

- **`account.id`를 UUID로 둔다**: 처음 검토 때 제안했던 안이다. 하지만 ADR-0003 #2와 충돌하고, Long `memberId`가 이미 박제된 계약(JWT, SSE)을 바꿔야 해서 기각한다.
- **GeekChat `users`를 신원으로 쓴다**: SSA가 `geekchat` 스키마에 묶이고, SSA의 논리 참조 9개에 매핑 계층이 필요하다.
- **SSA `member`를 신원으로 쓴다**: GeekChat FK 5개와 메시지 이력의 ID를 모두 다시 써야 하고, 실패하면 복구가 어렵다.
- **겹치는 username을 자동으로 바로 연결한다**: 다른 사람이 같은 username을 쓴 경우 남의 채팅 이력에 접근하게 된다. 그래서 연결 대기 단계를 둔다.

## 미결 (오너 결정 필요)

1. **순서**: ADR-0003 #10은 GeekChat 흡수(Stage 4)를 정체성 통합보다 먼저 하도록 정했다. Stage 4는 아직 끝나지 않았다. 다음 중 하나를 고른다.
   - (a) Stage 4를 먼저 끝낸다.
   - (b) P0과 P1(스키마, 데이터)만 먼저 하고, P2 이후는 Stage 4 뒤에 한다.
2. **겹치는 계정 3개**: 이름이 `e2e`, `test`로 시작해 테스트 계정으로 보인다(가정). 진짜 사용자인지, 아니면 정리 대상인지 정해야 한다.
3. **법적 보존 기간**: 익명화 전에 원본 개인정보를 보존해야 하는 기간이 있는지 확인이 필요하다.
4. **전환기 길이**: 옛 토큰을 함께 받아들이는 기간을 정해야 한다. 클라이언트 강제 업데이트 여부와 연결된다.
