# SSA Backend — HANDOFF

> 마지막 갱신: 2026-07-10 · 브랜치 `develop` (통합 완료 상태)
> 작업 트리: worktree `simple-schedule-app-be/.wt-ssa-analysis` (develop) · inner repo `simple-schedule-app-be/ssa` (main)

## 1. 지금 상태 한 줄 요약
열려 있던 PR 3개(#13/#14/#15) **전부 develop에 병합 완료**. develop = `031751c`, 3모듈 clean 빌드·테스트 green, CI green. 열린 PR 없음.

## 2. 이번 세션 병합 내역 (2026-07-10)
- **PR #15** `feature/1-4-input-validation` → develop (`dd076a2`): Jakarta Validation 제약 + 강의 정원 검증 강화.
- **PR #14** `feature/2-4-observability` → develop (`337bc82`): 관측성 인프라(Actuator/MDC/구조화 로깅), Redis 인프라 common 모듈 통합, 특강 4단계 동시성 방어(Redis Atomic→FairLock→낙관적 락→UK). 원래 main 타깃이었으나 develop로 재타깃.
  충돌 3건 해소: `SpecialLectureController`(@Valid import 유지, 데드 Redisson import 제거) · `TutorLectureEnrollmentServiceTest`(develop의 `message.type()` 검증 유지 — 병합 트리에 type 필드 존재 확인) · `InternalApiKeyFilter`(obs의 디렉터리 이동 휴리스틱을 무시하고 `course/.../config/`로 복원).
- **PR #13** `refactor/hexagonal-foundation` → develop (`031751c`): 헥사고날 기반 전체 — ArchUnit 래칫(Stage 0), notification 포트 본보기(Stage 1), course 11개 리포지토리 3분할(Stage 2), 인증 Phase 1, HANDOFF.
- **래칫 확장 1건** (`bc87f98`): `Schedule.version`(@Version/@Column) 위반 2건 freeze 등재 — obs 낙관적 락 유입분, 기존 JPA-in-domain 부채와 동일 범주로 Stage 3에서 일괄 해소. course 스토어 **151→153**.

## 3. 검증 상태
- 로컬: `./gradlew clean :common:build :course:build :notification:build` → BUILD SUCCESSFUL (develop `031751c`, 31 tasks)
- CI: PR #14 pass(2m41s) · PR #13 pass(3m1s) — PR #13은 재타깃 후 CI 미트리거 → close/reopen으로 트리거함 (재타깃 시 주의)
- 독립 리뷰: 머지 해소 4건 전부 APPROVE (Kafka type 체인, 필터 컴포넌트 스캔·prod 설정 생존까지 확인)

## 4. 미병합 잔여 브랜치 (PR 없음 — 다음 세션 과제)
obs(#14)에 **포함되지 않은** 별도 구현들. develop 기준 rebase + obs와 중복 판정부터 할 것.
- `feature/1-1-auth-enforcement` (+`stage/1-1`): 전 컨트롤러 @Auth+@RequireRole — **인증 Phase 3b. 오케스트레이터 sign-off 게이트 필요. API-CONTRACT.md 갱신 필수.**
- `feature/1-2-outbox-relay` (+`stage/1-2`): Outbox Relay Scheduler (at-least-once)
- `feature/1-3-optimistic-lock` (+`stage/1-3`): Lecture enrolledCount 낙관적 락 — obs 4단계 방어와 중복 가능성
- `feature/1-5-redis-lua-atomicity`: Redis Lua 원자화 — obs와 중복 가능성
- `feature/2-1-schedule-conflict` (+`stage/2-1`): 튜터/학생 스케줄 충돌 감지
- `test/select-lock-deadlock`: 실험용

## 5. 아키텍처 현황 (ADR-0002)
- **Stage 0**: 3모듈 `HexagonalRulesTest` + FreezingArchRule. 신규 위반 빌드 실패, 부채 단조 감소(예외: 병합 유입분은 사유 명시 후 등재).
- **Stage 1**: notification `EnrolledStudentsPort`, FCM → infrastructure.
- **Stage 2**: course 11개 리포지토리 3분할(port / JpaRepository / Adapter). 레이어 방향 위반 3모듈 0.
- 다음: Stage 2 나머지(notification 리포지토리 포트화, common testFixtures 정리) → Stage 3(JPA 애노테이션 도메인 제거 — **ADR-0003 작성 후**) → Stage 4(common 분해).
- 진행도 확인: `wc -l */src/test/archunit-violations/*` (course 153)

## 6. 주의/함정
- ArchUnit 위반 해소 시 freeze 스토어 갱신분을 **같은 커밋에**.
- common 테스트에 `DoNotIncludeJars` 금지(자기 main이 test-fixtures jar로 잡힘).
- `NotificationIntegrationTest` "총실패→DB저장" 1건 @Disabled(재설계 필요). 성능·데드락 테스트 `@Tag("slow")` — `-PincludeSlow`로 실행.
- 보안: 전 엔드포인트 무인증(IDOR) — 해결책은 feature/1-1이나 게이트 대기 중. 노출 시크릿(JWT/DB/WireGuard/SSH/FCM) 로테이션 미완 — `docs/SECURITY.md`.
- `getLectureEnrollments` 빈 강의 조회 404 스펙 — 프론트 확인 요망.
- 병합된 원격 브랜치(`chore/ai-infra-cicd`, `refactor/hexagonal-foundation`, `feature/1-4-*`, `feature/2-4-*`) 미삭제 — 삭제는 승인 후.
- 로컬 worktree: `.wt-ssa-analysis`(develop), `.wt-obs`(feature/2-4-observability, 정리 가능).

## 7. 검증 명령
```
cd simple-schedule-app-be/.wt-ssa-analysis
./gradlew :common:build :course:build :notification:build   # CI 스코프
# 전체 build는 :playground:test가 MySQL@3308 필요(환경 의존) — CI 대상 아님
```
로컬 인프라: `docker compose up -d` (MySQL 3306/3307, Redis 6379, Kafka 9092).

## 8. 문서 지도
- `docs/adr/0001` 멀티모듈 분리 · `docs/adr/0002` 헥사고날 채택+로드맵
- `docs/architecture/hexagonal-guidelines.md` 계층/포트 규약 + 리포지토리 3분할 패턴
- `docs/SECURITY.md` 보안 조치·로테이션·인증 계획
- `.planning/codebase/` 코드베이스 분석 7종 · 루트/모듈 `CLAUDE.md`
- `orchestration/API-CONTRACT.md` 클라이언트 계약 단일 기준
