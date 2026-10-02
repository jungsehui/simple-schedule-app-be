-- V5: GeekChat 사용자를 account에 잇는다 (ADR-0006 P1, 결정 1과 2)
--
-- GeekChat은 같은 DB의 `geekchat` 스키마에 있고 자체 Flyway가 없다. 그래서 신원 통합에 필요한
-- GeekChat 쪽 DDL과 백필을 SSA Flyway가 맡는다 (오너 결정, ADR-0006 P1).
--
-- 실행 조건:
--   · `geekchat` 스키마가 없다 (CI, 새 로컬 환경): 아무것도 하지 않는다.
--   · 스키마는 있는데 `geekchat.users`를 볼 수 없다: 권한 문제다. 조용히 건너뛰면 운영에서
--     "적용됨"으로 기록된 채 아무것도 안 된 상태가 되므로 실패시킨다.
--   · 그 밖: 아래를 실행한다. SSA DB 역할이 `geekchat.users`의 소유자여야 ALTER TABLE이 된다
--     (운영 적용 전 확인 쿼리: ADR-0006 P1 런북).
--
-- 처리:
--   1. geekchat.users.account_id (NULL 허용) 추가. NOT NULL 전환은 P4.
--   2. 소문자 username이 SSA 출신 account와 같은 GeekChat 사용자: 같은 사람으로 보되 본인 확인 전이라
--      account_link(PENDING)만 남기고 users.account_id는 비워 둔다 (결정 2).
--   3. 나머지 GeekChat 사용자: account_id_seq에서 새 id를 받아 account를 만들고 external_uuid에 UUID를
--      보존한 뒤 users.account_id를 채운다. 탈퇴 사용자는 익명화 상태 그대로 옮긴다 (결정 3).

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'geekchat') THEN
        RAISE NOTICE 'V5: geekchat 스키마가 없어 건너뛴다';
        RETURN;
    END IF;
    IF to_regclass('geekchat.users') IS NULL THEN
        RAISE EXCEPTION 'V5: geekchat 스키마는 있으나 geekchat.users를 찾을 수 없다. SSA DB 역할의 권한을 확인하라';
    END IF;

    ALTER TABLE geekchat.users ADD COLUMN IF NOT EXISTS account_id bigint;

    -- 2. 겹치는 username: 연결 대기
    INSERT INTO account_link (account_id, external_uuid, status, created_at)
    SELECT a.id, u.id, 'PENDING', now()
    FROM geekchat.users u
    JOIN account a ON a.external_uuid IS NULL AND a.username = lower(u.username)
    ON CONFLICT (external_uuid) DO NOTHING;

    -- 3. GeekChat 전용: 새 account
    INSERT INTO account (id, external_uuid, username, password_hash, email, status, created_at, updated_at, withdrawn_at)
    SELECT nextval('account_id_seq'),
           u.id,
           CASE WHEN u.status = 'WITHDRAWN' THEN NULL ELSE lower(u.username) END,
           NULL,
           CASE WHEN u.status = 'WITHDRAWN' THEN NULL ELSE u.email END,
           CASE WHEN u.status = 'WITHDRAWN' THEN 'WITHDRAWN' ELSE 'ACTIVE' END,
           u.created_at,
           u.updated_at,
           CASE WHEN u.status = 'WITHDRAWN' THEN COALESCE(u.deleted_at, u.updated_at) END
    FROM geekchat.users u
    WHERE NOT EXISTS (SELECT 1 FROM account_link l WHERE l.external_uuid = u.id)
    ON CONFLICT (external_uuid) DO NOTHING;

    UPDATE geekchat.users u
    SET account_id = a.id
    FROM account a
    WHERE a.external_uuid = u.id
      AND u.account_id IS NULL;
END $$;
