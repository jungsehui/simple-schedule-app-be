# 동시성 제어 4단계 방어 — K6 부하 테스트

선착순 특강 수강 신청에 대해 **방어 단계별 시나리오 비교**를 수치화하기 위한 K6 부하 테스트.

## 디렉토리

```
defense/
├── common/
│   ├── enrollment.js   # 공통 K6 모듈 (메트릭, 옵션, 분류 로직)
│   ├── setup.sql       # 테스트 데이터 (특강 1, 학생 10000)
│   ├── reset.sh        # 시나리오 간 DB+Redis 상태 초기화
│   └── verify.sh       # 종료 후 정합성 검증
├── scenario-a-baseline.js     # 1차 + 4차
├── scenario-b-with-lock.js    # 1차 + 2차 + 4차
└── scenario-c-full.js         # 1차 + 2차 + 3차 + 4차 (전체)
```

## 사전 준비

```bash
# 1. 인프라 띄우기 (MySQL 3306, Redis 6379, Kafka 9092)
docker compose up -d

# 2. 테스트 데이터 셋업
mysql -h 127.0.0.1 -P 3306 -uroot -p1234 course_db \
    < playground/k6-scripts/defense/common/setup.sql

# 3. Redis 정원 초기화 (특강 ID 1, 정원 100)
redis-cli SET special_lecture:1:available 100
```

## 시나리오 실행

각 시나리오는 **서버 환경변수가 다르므로**, 시나리오마다 서버를 다시 띄워야 한다.

### 시나리오 A — Baseline (1차 + 4차)

```bash
# 터미널 1: 서버
DEFENSE_DISTRIBUTED_LOCK=false \
DEFENSE_OPTIMISTIC_LOCK_HANDLING=false \
./gradlew :course:bootRun

# 터미널 2: 부하 테스트
bash playground/k6-scripts/defense/common/reset.sh
k6 run playground/k6-scripts/defense/scenario-a-baseline.js
bash playground/k6-scripts/defense/common/verify.sh A
```

### 시나리오 B — 분산 락 (1차 + 2차 + 4차)

```bash
# 터미널 1
DEFENSE_DISTRIBUTED_LOCK=true \
DEFENSE_OPTIMISTIC_LOCK_HANDLING=false \
./gradlew :course:bootRun

# 터미널 2
bash playground/k6-scripts/defense/common/reset.sh
k6 run playground/k6-scripts/defense/scenario-b-with-lock.js
bash playground/k6-scripts/defense/common/verify.sh B
```

### 시나리오 C — 전체 4단계 (권장)

```bash
# 터미널 1 (또는 환경변수 생략 — 기본값이 둘 다 true)
./gradlew :course:bootRun

# 터미널 2
bash playground/k6-scripts/defense/common/reset.sh
k6 run playground/k6-scripts/defense/scenario-c-full.js
bash playground/k6-scripts/defense/common/verify.sh C
```

## 측정 지표 해석

K6 출력 끝부분에 다음 메트릭이 누적된다:

| 메트릭                       | 의미                                      |
| --------------------------- | ----------------------------------------- |
| `enroll_success_count`      | HTTP 200 — 수강 신청 성공                 |
| `enroll_capacity_exceeded`  | 1차 Redis 거절 (정원 초과)                |
| `enroll_already_enrolled`   | 4차 UK 충돌 (동일 학생 중복)              |
| `enroll_lock_failed`        | 2차 분산 락 획득 실패 (waitTime 초과)     |
| `enroll_optimistic_failed`  | 3차 낙관적 락 충돌 (분산 락 비정상 시)    |
| `enroll_unknown_failed`     | 미분류 실패 (사후 분석 대상)              |
| `http_req_duration{p95}`    | p95 응답시간 (락 대기 포함)               |
| `iterations`                | 총 시도 (10,000)                          |

응답시간/TPS는 K6 내장 메트릭(`http_reqs`, `http_req_duration`)으로 자동 수집된다.

## 정합성 검증 (verify.sh)

테스트 종료 후 `verify.sh`가 다음을 확인한다:

```
DB enrollment count == capacity      → ✅ OK
DB enrollment count >  capacity      → ❌ 정원 초과 버그
DB enrollment count <  capacity      → ⚠️ 정원 유실 (부하 미달 또는 보상 과다)
Redis 잔여 != 0 && DB 정원 만료      → ⚠️ Redis↔DB 불일치
```

시나리오 A는 정원 초과/유실이 발생할 수 있고, 시나리오 B/C는 항상 정확히 100이어야 한다.

## 비교 테이블 작성 (블로그용)

3개 시나리오 결과를 다음 형식으로 정리:

| 지표                  | A (Baseline) | B (분산 락) | C (전체) |
| -------------------- | ------------ | ---------- | -------- |
| TPS                  | ?            | ?          | ?        |
| p50 / p95 / p99 (ms) | ? / ? / ?    | ? / ? / ?  | ? / ? / ?|
| 성공                 | ?            | ?          | ?        |
| 1차 거절 (capacity)  | ?            | ?          | ?        |
| 2차 락 실패          | -            | ?          | ?        |
| 3차 OptLock 실패     | -            | -          | ?        |
| **DB 정합성**         | ⚠️           | ✅         | ✅       |

핵심 인사이트:
- **A vs B**: 정합성을 얻기 위해 TPS를 어느 정도 trade-off 하는지 (수치화)
- **B vs C**: 낙관적 락 추가 비용이 거의 0이라는 것을 증명
- **A의 정합성 위반**: 부하 테스트로 race condition을 재현해 "왜 분산 락이 필요한지" 입증

## 알려진 한계

- 단일 머신에서 K6 + 서버 + DB가 함께 돌면 CPU/네트워크 경합으로 측정값이 이상할 수 있다.
  실제 운영 비교를 원하면 K6를 별도 머신/컨테이너로 분리할 것.
- `enroll_optimistic_failed` 계측은 ApplicationException 코드를 기반으로 한다.
  현재 OptimisticLockException → `SLE001`로 매핑되므로 분류 정확도를 높이려면
  `SpecialLectureEnrollmentExceptionCode.OPTIMISTIC_LOCK_FAILED` 신설을 고려하라(향후 개선).
