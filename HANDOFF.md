# SSA Backend — HANDOFF

> 마지막 갱신: 2026-07-06 · 브랜치 `refactor/hexagonal-foundation` (base: `chore/ai-infra-cicd`)
> 작업 트리: worktree `simple-schedule-app-be/.wt-ssa-analysis` · inner repo `simple-schedule-app-be/ssa`

## 1. 지금 상태 한 줄 요약
헥사고날 아키텍처 기반 구축 진행 중. **Stage 0(래칫)·Stage 1(외부기술 포트 본보기)·Stage 2(리포지토리 전면 포트화) 완료**, 전부 push됨. 빌드·테스트 3모듈 green.

## 2. 열려 있는 PR (모두 draft, 스택 구조)
- **PR #12** `chore/ai-infra-cicd` → `develop` : AI 인프라·CI/CD·배포·보안·Phase1 인증·테스트 안정화. CI **success** 확인됨.
- **PR #13** `refactor/hexagonal-foundation` → `chore/ai-infra-cicd` : 헥사고날 기반(ADR-0002, ArchUnit 래칫, 포트/어댑터). #12 머지 후 develop로 재타깃 예정.
- 머지 순서 제안: #12 → develop, 그다음 #13 재타깃 → develop.

## 3. 브랜치 커밋 (refactor/hexagonal-foundation, 위가 최신)
```
38f6c0d refactor(course): 남은 도메인 리포지토리 전면 포트화 (Stage 2 완료)
2485e4d refactor(course): member 리포지토리 포트화 (Stage 2 본보기)
bd490a4 test(arch): ArchUnit 헥사고날 규칙 + FreezingArchRule 래칫 (Stage 0)
121cfd8 refactor(notification): 헥사고날 본보기 — EnrolledStudentsPort, Firebase→infra
bbf5565 chore: 하우스키핑 (ngrinder bootJar off, local 시크릿 env화, gitignore glob)
8fd0dba feat(auth): 인증 Phase 1 (토큰/LoginResponse에 role, @Auth optional)
6a805e2 test(notification): EmbeddedKafka 랜덤 포트 격리 (CI)
```

## 4. 검증 명령
```
cd simple-schedule-app-be/.wt-ssa-analysis
./gradlew :common:build :course:build :notification:build   # CI 스코프, green
# 전체 build는 :playground:test가 MySQL@3308 필요(환경 의존) — CI 대상 아님
# 아키텍처 진행도(남은 부채 라인 수):
wc -l */src/test/archunit-violations/*
```
로컬 인프라: `docker compose up -d` (MySQL 3306/3307, Redis 6379, Kafka 9092).

## 5. 완료된 아키텍처 작업 (ADR-0002)
- **Stage 0**: 3모듈 `HexagonalRulesTest`(ArchUnit 1.3.0) + `FreezingArchRule` 스토어. 규칙: domain→application/presentation/config 금지, application→presentation 금지, 도메인 순수성(Spring/JPA/Firebase/Redisson/Kafka/jjwt/Jackson 금지), common auth↔messaging 독립. **신규 위반은 빌드 실패**, 부채는 단조 감소만.
- **Stage 1**: notification `EnrolledStudentsPort`(CourseClient=HTTP 어댑터), FCM 코드 `fcm.domain.service`→`fcm.infrastructure` 이동.
- **Stage 2**: course 11개 리포지토리 전부 3분할(port / infrastructure.persistence.XxxJpaRepository / XxxRepositoryAdapter). `MemberRegister`는 `MemberSubtypeRepository` 포트에만 의존(중복 예외 번역은 어댑터로). **course 스토어 177→151** (레이어 방향 위반은 3모듈 0).

## 6. 다음 할 일 (우선순위)
1. **Stage 2 나머지**: notification 리포지토리 포트화(fcm/notification/sse 도메인), common testFixtures 정리.
2. **Stage 3 (별도 ADR 필요)**: 도메인 엔티티에서 JPA 애노테이션 제거 — Member JOINED 상속 비용 큼, 착수 전 ADR-0003 작성. DomainEvent(JPA 엔티티+이벤트 추상화+outbox) 3분할.
3. **common 분해(Stage 4)**: common.auth(웹)·common.kafka/event(메시징)·common.domain(영속)을 독립 모듈로.
4. **인증 마이그레이션 Phase 2/3** (별도 게이트): Phase1 완료(role claim+LoginResponse.role+@Auth optional). Phase 3a=dual-read(@Auth+파라미터 병행), Phase 3b=파라미터 제거+인가 — **오케스트레이터 sign-off 후**. 계약: `orchestration/API-CONTRACT.md`.

## 7. 주의/함정
- ArchUnit 위반 해소 시 **freeze 스토어 갱신분을 같은 커밋에** 포함(안 하면 CI 혼란).
- common 테스트에 `DoNotIncludeJars` 쓰면 자기 main이 test-fixtures jar로 잡혀 스캔 0건 → 쓰지 말 것.
- notification `NotificationIntegrationTest`의 "총실패→DB저장" 1건 `@Disabled`(async static-mock 취약, 재설계 필요). 성능·데드락 테스트는 `@Tag("slow")`로 CI 제외(`-PincludeSlow`로 실행).
- 미해결 보안: 전 엔드포인트 무인증(IDOR), 노출 시크릿 로테이션(JWT/DB/WireGuard/SSH/FCM) — `docs/SECURITY.md`.
- `getLectureEnrollments`가 빈 강의 조회 시 이제 404(의도된 스펙, 전용 예외코드 존재) — 프론트 확인 요망.

## 8. 문서 지도
- `docs/adr/0001` 멀티모듈 분리 · `docs/adr/0002` 헥사고날 채택+로드맵
- `docs/architecture/hexagonal-guidelines.md` 계층/포트 규약 + 리포지토리 3분할 패턴
- `docs/SECURITY.md` 보안 조치·로테이션·인증 계획
- `.planning/codebase/` 코드베이스 분석 7종 · 루트/모듈 `CLAUDE.md`
- `orchestration/API-CONTRACT.md` 클라이언트 계약 단일 기준
