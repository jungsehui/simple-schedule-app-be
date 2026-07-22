-- V2: consultation_attendee.consultaition_id → consultation_id (FK 컬럼 오타 정정)
--
-- 배경: 운영 스키마는 Hibernate ddl-auto가 엔티티에서 생성했고, 엔티티의
-- @JoinColumn(name = "consultaition_id") 오타가 그대로 컬럼명이 됐다.
-- 이 마이그레이션과 같은 커밋에서 엔티티를 consultation_id로 정정한다.
--
-- 조건부 실행인 이유:
--   · 기존 운영 스키마: 오타 컬럼이 존재 → RENAME 실행.
--   · 새 환경(빈 스키마): Flyway가 ddl-auto보다 먼저 돌아 테이블 자체가 없음 → no-op.
--     (테이블·컬럼은 이후 ddl-auto가 정정된 엔티티 기준으로 생성 — V1 baseline 주석 참고)
-- RENAME COLUMN은 FK 제약·인덱스가 컬럼을 따라가므로 데이터/제약 손실이 없다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'consultation_attendee'
          AND column_name = 'consultaition_id'
    ) THEN
        ALTER TABLE consultation_attendee RENAME COLUMN consultaition_id TO consultation_id;
    END IF;
END $$;
