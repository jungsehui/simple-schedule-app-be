-- V1 baseline (ADR-0003 Stage 0)
-- 기존 운영 스키마(ssa_course)는 Hibernate ddl-auto가 생성한 상태를 baseline으로 동결한다.
-- baseline-on-migrate=true + baseline-version=1 이므로:
--   · 기존(비어있지 않은) 스키마: 이 파일은 실행되지 않고 V1로 기록만 된다.
--   · 새 환경(빈 스키마): 이 no-op 실행 후, 과도기 동안은 ddl-auto:update가 테이블을 생성한다.
-- 신규 DDL은 반드시 V2__*.sql 이상으로 추가할 것.
SELECT 1;
