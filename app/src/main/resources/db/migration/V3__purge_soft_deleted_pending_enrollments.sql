-- V3: 소프트 삭제 잔존 pending 행 정리 (하드 삭제 전환 후속 — main 963379f)
--
-- 하드 삭제 전환 이전에 소프트 삭제된 행이 남아 있으면:
--   · uk_pending_lecture_student가 deleted_date를 포함하지 않아 같은 조합의 재신청 INSERT가
--     500으로 실패하고,
--   · @SQLRestriction이 사라진 지금은 죽은 행이 정상 대기처럼 조회된다(잘못된 400 L5).
-- 전환 시점에 수동 정리가 안내됐지만(963379f 커밋 메시지), 실행 여부에 의존하지 않도록
-- 멱등 정리를 마이그레이션으로 보증한다. 하드 삭제 전환 후에는 deleted_date가 채워진 행이
-- 새로 생길 수 없으므로 이 정리는 1회로 충분하다.
--
-- 조건부인 이유: 새 환경은 정정된 엔티티(BaseDomain) 기준으로 테이블이 생성돼
-- deleted_date 컬럼 자체가 없다. (기존 스키마의 잔존 컬럼은 무해하므로 drop하지 않는다 —
-- ddl-auto:update는 컬럼을 제거하지 않고, 최소 변경을 유지한다.)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pending_lecture_enrollment'
          AND column_name = 'deleted_date'
    ) THEN
        DELETE FROM pending_lecture_enrollment WHERE deleted_date IS NOT NULL;
    END IF;
END $$;
