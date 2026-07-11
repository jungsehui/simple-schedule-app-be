# SSA Backend — HANDOFF

> 마지막 갱신: 2026-07-11 · 브랜치 `integration/geekchat-monorepo` (PR #17)
> 작업 트리: worktree `simple-schedule-app-be/.wt-ssa-analysis` · inner repo `simple-schedule-app-be/ssa` (main)

## 0. 최신: GeekChat 모노레포 + Supabase PG + 자동 배포 (PR #17)
- **GeekChat 편입**: geek-chat/server-v2 → `geekchat/` (subtree, 히스토리 보존). 한 레포 · 두 Gradle 빌드(Kotlin/Boot4 vs Java/Boot3.4). `common` 모듈명 충돌은 빌드 분리로 회피.
- **DB**: MySQL 컨테이너 제거 → **Supabase PostgreSQL 단일 외부 DB**, 스키마 격리(`ssa_course`/`ssa_notification`/`geekchat`). course prod PG 기동·DDL·네이티브 쿼리 로컬 PG16 검증 완료. ⚠ Supabase 접속은 **세션 풀러 호스트**(aws-0-ap-northeast-2.pooler.supabase.com:5432, user=postgres.&lt;ref&gt;) 사용 — direct 호스트는 IPv6 전용.
- **CD**: `deploy.yml`이 **main push에 자동 트리거** (develop→main PR 병합 = 배포). 이미지 3종(ssa-course/-notification/-geekchat) → GHCR → GH Actions가 WireGuard 터널로 서버 SSH 배포. nginx가 `/chat/`(REST)·`/chat/ws`(WebSocket)를 geekchat(context-path=/chat)로 라우팅.
- **배포 전 수동 준비 (1회)**: ① Supabase SQL Editor에서 스키마 3개 CREATE ② 서버 `/opt/ssa/.env`를 `deploy/.env.example` 기준으로 갱신(시크릿 전부 신규 발급 — 채팅에 노출된 Supabase 비밀번호·WG 키는 로테이션) ③ GitHub Secrets(WG_* 6 + DEPLOY_* 4) 등록 ④ 첫 배포만 `GEEKCHAT_DDL_AUTO=update` → validate 복귀 ⑤ 서버 SSH 접근 확인(shjung@10.64.212.20, WG 터널 경유).
- 잔여: notification PG 기동 스모크(FCM 키 필요), 서버 MySQL 기존 데이터 이관(범위 외, 빈 DB 시작), geekchat OAuth 콜백 URL 실도메인 반영.

## 1. 지금 상태 한 줄 요약 (develop 기준)
원격 29개 브랜치 전수 조사 → **유효 작업 전부 develop에 통합 완료**. develop = `a2dd683`, 3모듈 clean 빌드·테스트 green, CI green. PR #17(모노레포+PG+CD)이 develop 대기 중.

## 2. 이번 세션 통합 내역 (2026-07-10)
### 1차: 열린 PR 3개 병합
- **PR #15** input-validation (`dd076a2`) · **PR #14** observability+4단계 동시성 방어 (`337bc82`, main→develop 재타깃, 충돌 3건 해소) · **PR #13** 헥사고날 기반 (`031751c`) · 래칫 확장 `Schedule.version` 2건(151→153, `bc87f98`)

### 2차: 잔여 작업 브랜치 5종 적응 통합 (**PR #16**, `73e6c6e`)
병렬 분석 에이전트 7개가 실코드 기준으로 판정 후 순차 통합. 유닛별 전체 빌드 green 확인 후 커밋.
- `ab4fdb2` **Outbox Relay** (stage/1-2): 적응 — produce() @Transactional 제거(실패 상태 롤백 결함), retryCount 선영속(DEAD 도달 보장), @Column 미사용(래칫 위반 0)
- `82edda6` **스케줄 충돌 감지** (stage/2-1): 적응 — ScheduleRepository 3분할, ScheduleConflictValidator 순수 자바 + `config.ScheduleConfig` @Bean. 신규 409 SC0/SC1
- `fc9debb` **낙관적 락 재시도** (stage/1-3): 적응 — Lecture.@Version 드롭(Schedule 루트와 중복 → Hibernate 기동 실패 방지), @Retryable 3개 경로 유지
- `2363aa9` **Redis Lua 원자화** (feature/1-5): obs AtomicCounter 구조로 재구현 — 키부재(-2)/소진(-1) 구분, INCR-back 제거, TTL=종료+1일(과거면 30일). 신규 404 SL002
- `c3fb8e3` **인증 Phase 3a** (feature/1-1): 원안 3b 하드컷을 계약 게이트에 맞춰 **듀얼리드로 각색** — @RequireRole+RoleInterceptor(무토큰 통과, 불일치 403 T6), AuthIdentities(토큰 우선, 둘 다 없으면 401 T7), accept/reject 소유권 검증(토큰 제시 시만)

### 스킵 (사유 확정)
- `feature/sse` fix: develop에서 이관+소프트삭제로 해소 — 적용 시 재신청 차단 회귀
- `stage/1-4`: PR #15와 패치 동일(git cherry "-")
- `test/select-lock-deadlock`: 프로덕션 코드 160줄 삭제하는 실험 스냅샷

## 3. 검증 상태
- 로컬: `./gradlew clean :common:build :course:build :notification:build` → BUILD SUCCESSFUL (develop `73e6c6e`, 31 tasks)
- CI: PR #16 pass(2m51s) · develop push CI 감시 중(문서 커밋 포함 재확인 예정)
- 테스트 신규 20건: relay 3 · 충돌검증 3 · 재시도계약 1 · RedisClient 8(기존 5 갱신 — 사용자 승인) · RoleInterceptor 5 · AuthIdentities 3 · 소유권 1 (기존 accept/reject 2건 시그니처 갱신 — 사용자 승인)
- ArchUnit 래칫: course 153 유지, 신규 위반 0 (2-1의 신규 리포지토리는 3분할로 무위반 통합)

## 4. 계약 변경 (API-CONTRACT.md 반영 완료 — FE/오케스트레이터 공지 필요)
전부 additive (기존 클라이언트 무영향):
- 식별자 파라미터(tutorId/studentId/memberId) optional화 — `Authorization: Bearer` 시 토큰이 우선
- 403 T6 (토큰 제시+role 불일치) · 401 T7 (식별자 전무) · 400 L2 (소유권 위반, 기존 코드 신규 표면) · 409 SC0/SC1 (스케줄 충돌) · 404 SL002 (Redis 정원 키 부재)
- SSE는 경로 변수 폴백 유지 (EventSource 헤더 제약 — 3b 전 SSE 토큰 전송 방식 결정 필요)

## 5. 다음 할 일 (우선순위)
1. **인증 Phase 3b** (오케스트레이터 게이트): Android Phase 2 완료 후 — @Auth required 전환, 레거시 파라미터 제거, RoleInterceptor 무토큰 401. 코드에 `Phase 3a`/`3b에서 전환` 주석으로 전환 지점 전부 표시됨.
2. **Stage 2 나머지**: notification 리포지토리 포트화(fcm/notification/sse), common testFixtures 정리. common/event(DomainEventRepository)는 여전히 raw Spring Data — Stage 4(common 분해)와 함께.
3. **Stage 3** (ADR-0003 작성 후): 도메인 JPA 애노테이션 제거, 래칫 153 해소.
4. **운영 후속**: outbox DEAD 이벤트 메트릭/알림, domain_event.status 인덱스, 멀티 인스턴스 시 relay 중복 실행 가드(ShedLock), Redis 정원 키 백필 절차(SL002 대량 발생 대비), @Retryable 소진 시 409 매핑(@Recover).
5. **브랜치 청소** (승인 필요): 통합 완료된 원격 브랜치 삭제 — feature/1-1~1-5, 2-1, 2-4, stage/*, 병합된 PR 브랜치들.

## 6. 주의/함정
- ArchUnit freeze 갱신은 같은 커밋에 · common 테스트 DoNotIncludeJars 금지 · PR 재타깃만으론 CI 안 돎(close/reopen)
- 낙관적 락 @Version은 **Schedule 루트에만** — 자식 엔티티에 추가하면 Hibernate 기동 실패
- KafkaEventProducer.produce()에 @Transactional 금지 — 실패 상태 저장이 롤백됨
- Redis 정원 키 완전 소실(플러시) 시 특강 신청 전부 404 SL002 — 재적재 절차 필요
- 보안: 시크릿 로테이션(JWT/DB/WireGuard/SSH/FCM) 여전히 미완 — `docs/SECURITY.md`
- NotificationIntegrationTest 1건 @Disabled · slow 태그는 `-PincludeSlow`

## 7. 검증 명령
```
cd simple-schedule-app-be/.wt-ssa-analysis
./gradlew :common:build :course:build :notification:build   # CI 스코프
wc -l */src/test/archunit-violations/*                        # 래칫 진행도 (course 153)
```
로컬 인프라: `docker compose up -d` (MySQL 3306/3307, Redis 6379, Kafka 9092).

## 8. 문서 지도
- `docs/adr/0001` 멀티모듈 · `docs/adr/0002` 헥사고날 로드맵 · `docs/architecture/hexagonal-guidelines.md`
- `docs/SECURITY.md` 보안 · `.planning/codebase/` 분석 7종 · 루트/모듈 `CLAUDE.md`
- `orchestration/API-CONTRACT.md` 클라이언트 계약 단일 기준 (2026-07-10 Phase 3a 갱신됨)
