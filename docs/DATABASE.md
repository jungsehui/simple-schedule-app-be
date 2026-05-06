# Database Schema — MySQL 8.0

## 1. 개요

- **DBMS**: MySQL 8.0 (utf8mb4 / utf8mb4_unicode_ci)
- **이모지 지원**: utf8mb4 (4-byte UTF-8) — `🦊`, `😀` 등 정상 저장
- **타임존**: 모든 timestamp는 UTC (앱이 `Instant`로 다룸)
- **스키마 관리**: production은 `ddl-auto: validate` (사람이 관리), dev/bootstrap은 `update`
- **소프트 삭제**: 메시지/방은 `deleted_at` 컬럼 + Hibernate `@SQLRestriction`. **유저는 `status` enum** (`@ManyToOne` 조인 호환성 때문)

## 2. 테이블 목록

| 테이블 | 핵심 키 | 비고 |
|---|---|---|
| `users` | id (UUID PK), username UNIQUE | password_hash, status, soft delete 없음 (status로 처리) |
| `user_provider` | id PK, (provider, provider_id) UNIQUE | OAuth 연동 |
| `refresh_token` | id PK, token UNIQUE (length 64) | rotation, hard delete |
| `chat_room` | id PK | type, expires_at, deleted_at (soft delete) |
| `chat_room_member` | id PK, (user_id, chat_room_id) UNIQUE | 멤버십, joined_at, last_read_at |
| `message` | id PK, client_message_id UNIQUE | content, expires_at, deleted_at (soft delete + 메시지는 hard delete도 사용) |
| `invite_link` | id PK, code UNIQUE (length 8) | TTL + 사용 횟수 제한 |

## 3. 상세 스키마

### 3.1 `users`
```sql
CREATE TABLE users (
  id              VARCHAR(36)   NOT NULL PRIMARY KEY,
  nickname        VARCHAR(20)   NOT NULL,
  username        VARCHAR(20)            NULL UNIQUE,
  email           VARCHAR(255)           NULL,
  profile_image_url VARCHAR(1024)        NULL,
  password_hash   VARCHAR(255)           NULL,        -- BCrypt; OAuth-only 유저는 NULL
  status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | WITHDRAWN
  created_at      DATETIME(6)   NOT NULL,
  updated_at      DATETIME(6)   NOT NULL,
  deleted_at      DATETIME(6)            NULL,        -- 사용 안 함 (status로 대체)
  INDEX idx_user_username (username)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**중요**: 테이블명이 `users` (PG 시절 `"user"` → MySQL은 USER가 reserved function이라 변경).

### 3.2 `user_provider`
```sql
CREATE TABLE user_provider (
  id            VARCHAR(36)  NOT NULL PRIMARY KEY,
  user_id       VARCHAR(36)  NOT NULL,
  provider      VARCHAR(20)  NOT NULL,    -- GOOGLE | NAVER
  provider_id   VARCHAR(255) NOT NULL,
  email         VARCHAR(255)         NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_user_provider_provider UNIQUE (provider, provider_id),
  CONSTRAINT fk_user_provider_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.3 `refresh_token`
```sql
CREATE TABLE refresh_token (
  id          VARCHAR(36)  NOT NULL PRIMARY KEY,
  user_id     VARCHAR(36)  NOT NULL,
  token       VARCHAR(64)  NOT NULL UNIQUE,
  expires_at  DATETIME(6)  NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.4 `chat_room`
```sql
CREATE TABLE chat_room (
  id              VARCHAR(36)  NOT NULL PRIMARY KEY,
  type            VARCHAR(20)  NOT NULL,           -- DIRECT | GROUP
  name            VARCHAR(255)         NULL,       -- DIRECT는 NULL
  last_message_at DATETIME(6)          NULL,
  expires_at      DATETIME(6)          NULL,       -- 임시 방
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NOT NULL,
  deleted_at      DATETIME(6)          NULL        -- @SQLRestriction("deleted_at IS NULL")
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.5 `chat_room_member`
```sql
CREATE TABLE chat_room_member (
  id            VARCHAR(36)  NOT NULL PRIMARY KEY,
  user_id       VARCHAR(36)  NOT NULL,
  chat_room_id  VARCHAR(36)  NOT NULL,
  joined_at     DATETIME(6)  NOT NULL,
  last_read_at  DATETIME(6)          NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_member UNIQUE (user_id, chat_room_id),
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_member_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.6 `message`
```sql
CREATE TABLE message (
  id                VARCHAR(36)  NOT NULL PRIMARY KEY,
  chat_room_id      VARCHAR(36)  NOT NULL,
  sender_id         VARCHAR(36)  NOT NULL,
  client_message_id VARCHAR(255) NOT NULL UNIQUE,
  content           TEXT         NOT NULL,
  type              VARCHAR(20)  NOT NULL,         -- TEXT | SYSTEM
  expires_at        DATETIME(6)          NULL,    -- 자동삭제 메시지
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  deleted_at        DATETIME(6)          NULL,
  INDEX idx_message_room_created (chat_room_id, created_at DESC),
  INDEX idx_message_expires_at (expires_at),
  CONSTRAINT fk_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
  CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.7 `invite_link`
```sql
CREATE TABLE invite_link (
  id            VARCHAR(36)  NOT NULL PRIMARY KEY,
  code          VARCHAR(8)   NOT NULL UNIQUE,
  room_id       VARCHAR(36)  NOT NULL,
  creator_id    VARCHAR(36)  NOT NULL,
  expires_at    DATETIME(6)  NOT NULL,
  max_uses      INT                  NULL,
  current_uses  INT          NOT NULL DEFAULT 0,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  INDEX idx_invite_link_code (code),
  CONSTRAINT fk_invite_room FOREIGN KEY (room_id) REFERENCES chat_room(id),
  CONSTRAINT fk_invite_creator FOREIGN KEY (creator_id) REFERENCES users(id)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 4. Enum 매핑

JPA `@Enumerated(EnumType.STRING)`이라 enum 변경 시 컬럼 스키마는 그대로 (값만 추가됨).

| 도메인 | 값 | 컬럼 |
|---|---|---|
| `UserStatus` | `ACTIVE`, `WITHDRAWN` | `users.status` |
| `AuthProvider` | `GOOGLE`, `NAVER` | `user_provider.provider` |
| `ChatRoomType` | `DIRECT`, `GROUP` | `chat_room.type` |
| `MessageType` | `TEXT`, `SYSTEM` | `message.type` |

## 5. Soft Delete 전략

| 테이블 | 전략 | 이유 |
|---|---|---|
| `users` | **status enum** (`ACTIVE`/`WITHDRAWN`) | `@ManyToOne`이 User 참조 → soft delete 시 조인이 NULL 반환되어 `toDomain()` 깨짐 |
| `chat_room` | `deleted_at` + `@SQLRestriction` | 표준 |
| `message` | `deleted_at` + `@SQLRestriction` (`messageRepository.softDeleteByRoomId`), 일부는 hard delete (`hardDeleteByIds` for 자동삭제) | 만료 메시지는 hard delete (스토리지 절약) |
| `chat_room_member` | hard delete | 멤버십은 명시적으로 끊기 |
| `user_provider`, `refresh_token`, `invite_link` | hard delete | 보안/공간 |

## 6. 인덱스 정책

자주 쓰이는 쿼리 기준:

| 인덱스 | 쿼리 |
|---|---|
| `idx_user_username` | username 검색 / signup 중복 체크 |
| `idx_message_room_created` | 메시지 커서 페이징 (room별 최신순) |
| `idx_message_expires_at` | 자동삭제 스케줄러 (`WHERE expires_at <= now`) |
| `idx_invite_link_code` | 코드로 초대 링크 조회 |
| `uk_member` | 멤버십 중복 방지 + 유저별 방 조회 |
| `uk_user_provider_provider` | OAuth provider 매칭 |

추가 인덱스 후보 (필요 시):
- `chat_room.expires_at` — 만료 방 스케줄러용 (현재 풀스캔이지만 방 수 적어 OK)
- `users.email` — 이메일 검색 (기능 추가 시)

## 7. v1 → v2 데이터 마이그레이션 (필요 시)

v1은 PostgreSQL이고 v2는 MySQL이므로 직접 dump/restore 안 됨.

```bash
# 1. v1에서 SQL dump
pg_dump -h <v1-host> -U <user> -d geekchat \
  -t users -t user_provider -t refresh_token \
  --data-only --column-inserts > v1-dump.sql

# 2. 변환 (수동 or sed):
#    - PG bool → MySQL TINYINT
#    - PG TIMESTAMPTZ → MySQL DATETIME(6) (UTC 가정)
#    - PG quoted "user" → MySQL `users` (테이블명 변경)

# 3. MySQL에 적용
mysql -u root -p geekchat < v1-dump-converted.sql

# 4. 마이그레이션된 유저는 password_hash NULL → 비밀번호 찾기 흐름으로 유도
```

현재까지는 v1에 운영 데이터 없음 → 마이그레이션 작업 불필요.

## 8. 첫 부팅 (스키마 생성)

새 호스트에 처음 배포할 때:
1. `application.yml` default 프로필을 임시로 `ddl-auto: update`로 변경 (또는 환경변수 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`)
2. `docker compose up` → Hibernate가 7개 테이블 자동 생성
3. 부팅 성공 확인 후 `validate`로 복귀
4. (옵션) `mysqldump --no-data geekchat > schema.sql`로 스키마 백업

## 9. 자주 쓰는 쿼리

```sql
-- 활성 유저 수
SELECT COUNT(*) FROM users WHERE status = 'ACTIVE';

-- 유저별 메시지 수 Top 10
SELECT sender_id, COUNT(*) AS cnt
FROM message
WHERE deleted_at IS NULL
GROUP BY sender_id
ORDER BY cnt DESC
LIMIT 10;

-- 만료 임박 방 (10분 이내)
SELECT id, name, expires_at
FROM chat_room
WHERE deleted_at IS NULL
  AND expires_at IS NOT NULL
  AND expires_at BETWEEN NOW(6) AND DATE_ADD(NOW(6), INTERVAL 10 MINUTE);

-- 활성 초대 링크
SELECT code, room_id, current_uses, max_uses, expires_at
FROM invite_link
WHERE expires_at > NOW(6)
  AND (max_uses IS NULL OR current_uses < max_uses);
```

## 10. 백업/복원

```bash
# 백업
docker compose exec db mysqldump -u root -p${DB_ROOT_PASSWORD} geekchat | gzip > backup.sql.gz

# 복원 (주의: 기존 DB 비운 후)
gunzip -c backup.sql.gz | docker compose exec -T db mysql -u root -p${DB_ROOT_PASSWORD} geekchat
```

DEPLOYMENT.md 4절(롤백)과 함께 사용.
