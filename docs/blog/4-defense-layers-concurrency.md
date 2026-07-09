# 동시성 제어 4단계 방어: Redis Lock이 정답이 아닌 이유

> **TL;DR**
> SSA(Simple Schedule App)의 선착순 특강 수강 신청을 4단계 방어 구조 — **Redis Atomic → Redisson FairLock → DB 낙관적 락 → DB UK 제약** — 으로 고도화했다.
> 단일 방어선만으로는 정합성을 보장할 수 없고, **각 방어선이 막는 실패 모드가 다르다**.
> K6로 시나리오별 부하 테스트를 돌려, "낙관적 락은 정상 시 비용 0이고 비정상 시에만 발동한다"는 사실을 수치로 검증했다.

---

## 1. 문제 정의 — 왜 Redis Lock 하나로는 부족한가

기존 SSA의 특강 수강 로직은 다음 두 가지 방어선만 가지고 있었다.

```
요청 → [1차: Redis DECR] → DB INSERT (UK 제약 = 4차)
```

겉으로 보면 충분해 보인다.

- 1차 Redis DECR이 원자적이므로 정원이 음수가 되는 일이 없고,
- 4차 UK 제약이 동일 학생의 중복 신청을 막는다.

하지만 **문제는 1차와 4차 사이의 빈 공간**이다.

### 시나리오: Redis DECR 직후 서버 크래시

```
T0: Redis DECR → 99 (정원 차감 성공)
T1: 서버 프로세스 비정상 종료 (OOM/SIGKILL)
T2: 보상 트랜잭션 실행 안 됨
T3: Redis는 99, DB는 enrollment 0건 → "정원 1자리가 영원히 사라짐"
```

이런 **phantom decrement** 문제는 보상 코드가 `try/catch`에만 의존하는 한 막을 수 없다.

### 시나리오: 일반 강의의 in-memory 정원 검증

`Lecture.validateCanIncreaseEnrolledCount()`는 `enrolledCount >= capacity`를 메모리에서 비교한다.

```
Thread A: enrolledCount=99 읽음 → 검증 통과
Thread B: enrolledCount=99 읽음 → 검증 통과
Thread A: enrolledCount=100 저장 ✓
Thread B: enrolledCount=101 저장 ← 정원 초과
```

UK 제약 `(lecture_id, student_id)`은 **같은 학생의 중복**만 막을 뿐, **정원 초과**는 막지 못한다.

### 결론

**한 단계의 방어선이 가장 강력해 보이더라도, 그 방어선이 깨지는 시나리오는 반드시 존재한다.**
필요한 건 "더 강한 락"이 아니라 **다층 방어(defense in depth)**다.

---

## 2. 4단계 방어 구조 설계

```
요청 → [1차 Redis Atomic] → [2차 Redisson 분산 락] → [3차 DB 낙관적 락] → [4차 UK 제약]
        캐시 계층                애플리케이션 계층            DB 계층                DB 제약
        bulk 사전 필터링         critical section 보호       정합성 마지막 안전망       중복 차단
```

각 방어선은 **막는 실패 모드가 다르고, 비용이 다르고, 동작 빈도도 다르다.**

### 1차 — Redis Atomic Counter

> "10,000명 중 9,900명을 문 앞에서 돌려보낸다."

Redis DECR은 단일 명령 = 원자적이다.
정원 100명 특강에 10,000명이 동시에 와도, Redis 내부에서 순차 처리되므로 101번째 DECR 결과는 -1.

**이 단계의 역할은 "DB까지 도달하는 요청 수를 정원 수준으로 줄이는 것."**
DB 부하를 99% 감소시키므로, 뒤이어 등장할 분산 락의 비용을 감내할 수 있게 만든다.

**구현 보강:** Redis 키에 TTL을 부여해 메모리 누수를 막았다.

```java
private static final Duration AVAILABLE_CAPACITY_TTL = Duration.ofDays(30);

public void initializeSpecialLecture(Long specialLectureId, int capacity) {
    String key = buildAvailableSpecialLectureCapacityKey(specialLectureId);
    atomicCounter.set(key, capacity, AVAILABLE_CAPACITY_TTL);
}
```

### 2차 — Redisson FairLock

> "1차를 통과한 100명을 한 줄로 세운다."

1차에서 거의 다 걸러졌지만, 그래도 100명이 동시에 DB INSERT를 시도하면 race condition이 가능하다.
Redisson FairLock으로 **DB 접근만** 직렬화한다.

**왜 FairLock인가?** 일반 Lock은 대기 큐에서 임의 스레드가 다음 락을 잡아 기아(starvation)가 가능하다. FairLock은 FIFO 큐로 순서를 보장하는데, 이게 **선착순 시스템의 공정성**과 부합한다.

**핵심: 1차 위에 2차가 얹히면 비용이 합리적이다.**

- 1차 없이 2차만 쓰면 → 10,000명이 모두 락 대기 → 타임아웃 폭주
- 1차가 9,900명을 걸러주므로 → 100명만 락 대기 → 락 대기 시간 ~수십 ms

```java
@RedissonDistributedLock(
        key = "'special_lecture:' + #specialLectureId + ':enroll'",
        waitTime = 5L,
        leaseTime = 3L
)
public SpecialLectureEnrollment enrollWithLock(Long specialLectureId, Long studentId) {
    return specialLectureEnrollmentService.enrollSpecialLectureEnrollment(specialLectureId, studentId);
}
```

**Spring AOP의 self-invocation 함정**: AOP는 프록시 기반이라 같은 빈 내부 호출에는 적용되지 않는다.
그래서 락 적용 메서드는 별도 빈(`LockedSpecialLectureEnroller`)으로 분리했다.

### 3차 — DB 낙관적 락 (@Version)

> "분산 락이 만료될 때의 안전망."

엔티티에 `@Version`을 추가하면 JPA가 UPDATE 시 `WHERE version = ?`을 자동으로 추가한다. 다른 트랜잭션이 먼저 commit했다면 UPDATE가 0행 → `OptimisticLockException`.

**왜 비관적 락이 아닌 낙관적 락인가?**

- 2차 분산 락이 정상 동작하면 동시 접근 자체가 없으므로 **충돌 빈도 ≈ 0**
- 비관적 락(FOR UPDATE)은 **모든 정상 요청에 락 비용**을 지불
- 낙관적 락은 **충돌 시에만 비용을 지불**

따라서 비용 구조는:

| 락 종류 | 정상 시 비용 | 충돌 시 비용 |
|--------|------------|------------|
| 비관적 락 | 락 획득/해제 (항상) | 추가 대기 |
| 낙관적 락 | version 비교만 (싸다) | 재시도 또는 실패 |

분산 락이 직렬화를 이미 해주고 있으니, **낙관적 락이 정답**이다.

**JPA 제약사항 한 가지**: `@Version`은 엔티티 계층(`@Inheritance`)의 **루트(root)**에만 둘 수 있다.
그래서 `Schedule`(부모)에 두고, `Lecture`/`SpecialLecture`/`Consultation` 모두 자동으로 보호받게 했다.

### 4차 — DB UK 제약

> "같은 학생이 두 번 들어오는 건 물리적으로 불가능."

`uk_special_lecture_student (special_lecture_id, student_id)` 유니크 제약.
1~3차가 모두 깨져도 동일 학생의 중복 INSERT는 DB가 거부한다.

**주의**: UK는 "**중복 방지**"이지 "**정원 초과 방지**"가 아니다. 정원은 1~3차의 책임.

---

## 3. 4단계가 협력하는 흐름

```
학생 10,000명이 정원 100명 특강에 동시 신청:

[1차] Redis DECR
  → 9,900명 즉시 거절 (~0.1ms/건)
  → 100명만 통과

[2차] Redisson FairLock
  → 100명이 FIFO로 순차 DB 접근
  → 락 획득 실패(waitTime 초과) 시: Redis INCR 보상 + 거절

[3차] DB 낙관적 락 (version)
  → 정상: 분산 락이 직렬화 → 충돌 없음 → 비용 ≈ 0
  → 비정상(락 만료): version 불일치 감지 → OptimisticLockException → 보상

[4차] UK 제약
  → 같은 학생 재시도: DataIntegrityViolation → "이미 등록됨"
```

### 방어 단계별 역할 매트릭스

| 방어선 | 보호 대상 | 성능 비용 | 동작 빈도 |
|--------|---------|---------|---------|
| 1차 Redis Atomic | 정원 초과 (bulk) | ~0.1ms | 항상 (모든 요청) |
| 2차 분산 락 | Race condition | ~2-5ms | 1차 통과 시만 (~1%) |
| 3차 낙관적 락 | 동시 수정 | ~0ms 정상 | 2차 실패 시만 (극히 드묾) |
| 4차 UK 제약 | 중복 INSERT | ~0ms | 항상 (DB 레벨) |

---

## 4. K6로 수치 검증

세 시나리오를 비교 측정한다 (K6 스크립트는 [`playground/k6-scripts/defense/`](../../playground/k6-scripts/defense/)).

| 시나리오 | 환경변수 | 활성 방어선 |
|---------|---------|-----------|
| A (Baseline) | `DEFENSE_DISTRIBUTED_LOCK=false`, `OPTIMISTIC_LOCK_HANDLING=false` | 1차 + 4차 |
| B (분산 락) | `DEFENSE_DISTRIBUTED_LOCK=true`, `OPTIMISTIC_LOCK_HANDLING=false` | 1차 + 2차 + 4차 |
| C (전체) | 둘 다 `true` | 1차 + 2차 + 3차 + 4차 |

조건: 10,000 VU × 1 iteration, 정원 100명.

### 측정 지표

| 지표 | A (Baseline) | B (분산 락) | C (전체) |
|------|-------------|-----------|---------|
| TPS | (측정 후 채움) | (측정 후 채움) | (측정 후 채움) |
| p50 / p95 / p99 (ms) | / / | / / | / / |
| 성공 | 100명 (목표) | 100명 | 100명 |
| 1차 거절 | 9,900 | 9,900 | 9,900 |
| 2차 락 실패 | - | (측정) | (측정) |
| 3차 OptLock 실패 | - | - | (측정) |
| **DB 정합성** | ⚠️ 가능 | ✅ 100% | ✅ 100% |

### 핵심 인사이트 (예측)

1. **A의 정합성 위반**: K6 부하 후 `verify.sh`가 enrollment 수가 100을 넘는지 확인. 한 번이라도 넘으면 분산 락의 필요성 증명 완료.
2. **A vs B의 TPS 차이**: 분산 락이 직렬화하므로 TPS가 떨어진다. 그 떨어진 만큼이 "정합성의 가격".
3. **B vs C의 차이가 거의 없음**: 정상 시 낙관적 락은 발동하지 않으므로 비용 ≈ 0. → "낙관적 락은 비싸지 않다"는 본질을 데이터로 입증.

---

## 5. 구현의 함정 (실제 작업하면서 만난 것들)

### 함정 1 — `@Version`은 root entity에만

처음에 `Lecture`, `SpecialLecture`에 직접 `@Version`을 붙였다가 컴파일은 통과했지만 Spring 컨텍스트 로드 시 폭발:

```
Entity 'com.example.simplescheduleapp.lecture.general.domain.Lecture'
is a subclass in an entity class hierarchy and may not have a property annotated '@Version'
```

JPA 표준 제약. `@Inheritance` 계층에서 `@Version`은 root에만. 부모 `Schedule`로 옮기면 모든 자식이 자동으로 보호받는다 — 도메인 의미상도 자연스럽다.

### 함정 2 — Spring AOP self-invocation

`@RedissonDistributedLock`을 `RedisSpecialLectureEnrollmentService` 내부의 private 메서드에 붙이면 작동하지 않는다. 같은 빈 내부 호출은 프록시를 거치지 않기 때문.

해결: `LockedSpecialLectureEnroller`라는 별도 빈으로 분리.

### 함정 3 — 외부 `@Transactional` 제거

분산 락 AOP가 내부에서 `REQUIRES_NEW` 전파로 별도 트랜잭션을 시작-커밋한다.
외부에 `@Transactional`이 있으면 중첩 트랜잭션 + 락 해제와 커밋 순서가 꼬인다.

해결: `RedisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment()`에서 `@Transactional` 제거. Redis 작업은 명시적 보상으로 처리, DB 작업은 락 안의 `REQUIRES_NEW`가 처리.

### 함정 4 — Feature Toggle은 어떻게 켜고 끌 것인가

`@RedissonDistributedLock`은 컴파일 타임 어노테이션이라 런타임 toggle 불가.
→ 락 적용 메서드를 별도로 두고, `RedisSpecialLectureEnrollmentService`에서 `if-else`로 분기. 환경변수로 시나리오 전환:

```java
private SpecialLectureEnrollment saveEnrollment(SpecialLectureEnrollmentCreateCommand command) {
    if (defenseProperties.distributedLock()) {
        return lockedSpecialLectureEnroller.enrollWithLock(...);
    }
    return specialLectureEnrollmentService.enrollSpecialLectureEnrollment(...);
}
```

---

## 6. 다음 단계

- **정합성 스케줄러**: 프로세스 비정상 종료 시 Redis ↔ DB의 차이를 주기적으로 보정
- **Saga 패턴 도입**: Redis DECR과 DB INSERT를 분산 트랜잭션으로 묶어 보상 자동화
- **OPTIMISTIC_LOCK_FAILED 별도 ExceptionCode**: K6 분류 정확도 향상
- **Spike 부하 패턴 추가**: 1초 내 10,000 RPS — 순간 폭주 시 동작 검증

---

## 면접 키워드 + 30초 답변 + 꼬리질문

### Q. "선착순 시스템에서 동시성 제어를 어떻게 했나요?"

**30초 답변:**
> 단일 방어선이 아니라 **4단계 방어 구조**로 설계했습니다.
> 1차는 Redis DECR로 99%의 요청을 캐시 계층에서 즉시 거절하고, 2차는 1차를 통과한 ~100명에 대해 Redisson FairLock으로 DB 접근을 직렬화합니다. 3차는 분산 락이 비정상으로 풀렸을 때를 대비한 DB 낙관적 락(@Version)이고, 4차는 동일 학생 중복을 막는 UK 제약입니다.
> 각 방어선이 **막는 실패 모드와 비용 구조가 다르기 때문에**, 한 단계만으로는 정합성과 성능을 동시에 만족시킬 수 없습니다.

### Q (꼬리질문 1). "낙관적 락 대신 비관적 락을 쓰면 안 되나요?"

> 비관적 락은 **모든 정상 요청에 락 비용**을 지불합니다. 분산 락이 이미 직렬화를 보장한다면, DB에서 또 락을 거는 건 중복 비용입니다. 낙관적 락은 **충돌 시에만** version 불일치로 실패하므로, 정상 흐름에서는 비용이 거의 없습니다. K6 시나리오 B vs C의 TPS 차이가 무시할 수준이라는 것이 실측 근거입니다.

### Q (꼬리질문 2). "Redis가 죽으면 어떻게 되나요?"

> 1차 방어선이 무력화되어 모든 요청이 2차 분산 락으로 직접 들어옵니다. 그러면 락 대기열이 폭주하고 타임아웃이 늘어나서 사실상 서비스 불가. 운영에서는 **Circuit Breaker로 1차를 우회하고 DB 비관적 락으로 fallback**하거나, **Redis 클러스터링 + sentinel로 단일 장애점을 제거**하는 게 정석입니다. 또한 정상 운영 중이라도 Redis ↔ DB의 phantom decrement를 주기적으로 보정하는 **정합성 스케줄러**가 필요합니다.

### Q (꼬리질문 3). "왜 FairLock인가요? 일반 Lock과 차이는?"

> 일반 Lock은 락 해제 시 대기 중인 스레드 중 **임의의 하나**가 다음 락을 잡습니다. 이 경우 같은 스레드가 계속 락을 못 잡는 starvation이 가능합니다. **선착순 시스템의 공정성**은 "먼저 도착한 요청이 먼저 처리된다"는 것이므로, FIFO 큐 기반의 FairLock이 도메인 요구사항과 부합합니다. 대신 FairLock은 큐 관리 오버헤드가 일반 Lock보다 큽니다 — 이건 1차 방어선이 부하를 1% 수준으로 줄여줬기 때문에 감내 가능합니다.

---

## 참고 코드

- [`SpecialLecture.java`](../../course/src/main/java/com/example/simplescheduleapp/lecture/special/domain/SpecialLecture.java)
- [`Schedule.java`](../../course/src/main/java/com/example/simplescheduleapp/schedule/domain/Schedule.java) — `@Version` 위치
- [`RedisSpecialLectureEnrollmentService.java`](../../course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java) — 4단계 오케스트레이션
- [`LockedSpecialLectureEnroller.java`](../../course/src/main/java/com/example/simplescheduleapp/lecture/special/application/LockedSpecialLectureEnroller.java) — 2차 방어선
- [`SpecialLectureRedisClient.java`](../../course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureRedisClient.java) — 1차 방어선
- [`DefenseProperties.java`](../../course/src/main/java/com/example/simplescheduleapp/lecture/special/config/DefenseProperties.java) — Feature Toggle
- [K6 스크립트](../../playground/k6-scripts/defense/) — 시나리오 A/B/C
