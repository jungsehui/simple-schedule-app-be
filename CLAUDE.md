# simple-schedule-app-be (SSA)

과외·강의 수강신청 스케줄 앱 백엔드. **Spring Boot 3.4.3 / Java 21 / Gradle 멀티모듈.**

## 모듈 구조

| 모듈 | 역할 | 포트 |
|---|---|---|
| `common/` | 공유 라이브러리 (bootJar 비활성). Kafka 이벤트·DTO, JWT(jjwt), 공통 예외, testFixtures(Fixture Monkey) | - |
| `course/` | 수강/강의/회원 API 서버. 도메인: member, student, tutor, parent, lecture(general/special), schedule, consultation | 8080 |
| `notification/` | 알림 서버. SSE 스트림(`/sse-stream/{memberId}`), FCM 푸시, Kafka 컨슈머 | 8081 |
| `playground/`, `ngrinder/` | 부하 테스트 전용 — CI/배포 대상 아님 | - |

## 빌드·테스트 명령

```bash
./gradlew :common:build :course:build :notification:build --parallel   # CI와 동일
./gradlew :course:test --tests '*SpecialLecture*'                      # 단일 테스트
docker compose up -d    # 로컬 인프라 (MySQL 3306/3307, Redis 6379, Kafka 9092)
```

- 테스트는 **H2 + @EmbeddedKafka** 기반 — 외부 인프라 없이 실행 가능.
- 테스트 픽스처는 `common`의 testFixtures + Fixture Monkey 사용.

## 아키텍처 규칙 (위반 금지)

1. **레이어 방향**: `presentation → application → domain`. 역방향 의존 금지. 도메인별 패키지 안에 `presentation/application/domain/exception` 레이어 구성.
2. **모듈 간 통신**: 비동기는 Kafka 이벤트(common의 이벤트 클래스), 동기는 course의 `/internal/**` REST(외부 노출 금지 — nginx에서 403 차단).
3. **동시성 제어**: course는 Redisson 분산락 + DB 락 계층 방어(특강 수강신청은 4단계 방어). 락 순서 변경 시 `test/select-lock-deadlock` 브랜치의 데드락 사례 참고.
4. **SSE**: notification은 `open-in-view: false` 유지 (Emitter 커넥션 고갈 방지).
5. **시크릿**: 코드·yml에 하드코딩 금지. 운영 값은 환경변수(`application-prod.yml` 패턴), FCM JSON은 gitignore 유지.

## 브랜치 전략

- `main` = 스켈레톤(비어 있음), `develop` = 통합 브랜치, `feature/*` = 작업 브랜치.
- 실제 최신 코드: `feature/query-performance-tuning` (develop 포함). `feature/2-4-observability`는 별도 계열.
- 커밋 컨벤션: `feat:|fix:|refactor:|test:|docs:` + 한국어 요약.

## 문서 맵 (AI 에이전트는 작업 전 필독)

- `.planning/codebase/` — 코드베이스 분석 7종 (STACK, ARCHITECTURE, STRUCTURE, CONVENTIONS, TESTING, INTEGRATIONS, CONCERNS)
- 각 모듈의 `CLAUDE.md` — 모듈별 상세 가이드
- `docs/adr/` — 아키텍처 결정 기록(ADR)
- `deploy/README.md` — 배포 런북 (GitHub Actions → GHCR → WireGuard → SSH)

## 전용 에이전트 (.claude/agents/)

| 에이전트 | 용도 |
|---|---|
| `ssa-code-analyzer` | 코드베이스 분석·영향도 조사 (읽기 전용) |
| `ssa-architecture-guardian` | 레이어/의존성/DDD 규칙 준수 리뷰 (읽기 전용) |
| `ssa-prod-builder` | 컨벤션을 따르는 상용 코드 구현 |
| `ssa-qa-reviewer` | 테스트·보안·성능 QA 리뷰 (읽기 전용) |

구현 작업 흐름: analyzer로 영향도 파악 → prod-builder로 구현 → guardian + qa-reviewer로 이중 검증.
