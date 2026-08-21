# notification 모듈 Clean Architecture 정리

## Context

ADR-0004가 도메인은 순수화했지만(ArchUnit freeze 0, 도메인 프레임워크 import 0) **모듈 구조는 절반만 정리됐다.** notification 모듈에는 레이어(`domain`/`application`/`infrastructure`/`presentation`) 밖에 있는 클래스가 **20개** 있고, ArchUnit 규칙이 `..domain..` 패키지만 검사하므로 **이 20개는 규칙의 사각지대**다. freeze 0은 "구조가 깨끗하다"가 아니라 "검사 대상이 아니다"였다.

레이어가 정착된 슬라이스는 `fcm`/`notification`/`sse` 셋이다. 그 밖의 `redis/*`, `kafka/consumer`, `*/strategy`, `*/schedule`, `sse/cache`, `fcm/utils`가 정리 대상이다.

**실측 근거** (`origin/develop`):
- 레이어 밖 java 파일 20개 (exception 3개 제외 시 이동 대상 16개)
- `NotificationStrategy.handle(KafkaLectureEventMessage)` — 유스케이스 인터페이스가 **Kafka DTO에 직접 의존**
- `SseEmitterRepository`는 이름만 Repository이고 `SseEmitter`(Spring Web 타입)를 담는 인메모리 맵
- redis 사용처: sse 3 / notification 1

## Global Constraints

1. **동작 보존.** Task 1~3은 순수 이동/규칙 추가다. 로직·시그니처·빈 이름을 바꾸지 않는다. Task 4만 의도적 설계 변경이다.
2. **exception 패키지는 건드리지 않는다.** `fcm/exception`, `notification/exception`, `sse/exception`은 레이어 밖에 두는 것이 course 모듈과 동일한 관행이다 — 일관성이 개별 최적화보다 중요하다.
3. **테스트는 스펙이다.** 이동으로 테스트가 깨지면 프로덕션 코드(경로/import)를 고친다. 단, 이동에 따른 import 갱신은 테스트 파일에도 적용한다(리팩터 종속 적응).
4. **각 Task는 독립 revert 가능해야 한다.** Task 경계에서 빌드가 green이어야 한다.
5. 빌드 검증은 `./gradlew :common:build :course:build :notification:build :app:build`.
6. 패키지 이동 시 **참조하는 모든 파일의 import를 함께 갱신**한다. 실측 참조 수: strategy 8, redis.cache 7, sse.cache 6, kafka.consumer 6, redis.publisher 4, redis.topic 4, notification.schedule 3, fcm.utils 2, redis.subscriber 2.

---

## Task 1: ArchUnit 규칙 확대 — 사각지대 제거

**목표:** 레이어 밖 클래스를 규칙이 보게 만든다. 이 규칙이 Task 2·3의 진행률 계측기가 된다.

**파일:** `notification/src/test/java/com/example/simplescheduleapp/architecture/HexagonalRulesTest.java`

**추가할 규칙** (기존 3개 규칙은 그대로 두고 하나 추가):

```java
/**
 * 모든 프로덕션 클래스는 레이어 패키지 안에 있어야 한다.
 *
 * <p>기존 규칙이 `..domain..`만 검사해서, 레이어 밖 클래스(redis/·kafka/consumer·strategy 등)는
 * 아예 규칙의 사각지대였다 — freeze 0이 "구조가 깨끗하다"를 뜻하지 않았다.
 * exception 패키지는 course 모듈과 동일한 관행이라 의도적으로 허용한다.
 */
@ArchTest
static final ArchRule classes_should_live_in_a_layer = freeze(
        classes().that().resideOutsideOfPackages("..architecture..", "..exception..")
                .should().resideInAnyPackage("..domain..", "..application..", "..infrastructure..", "..presentation..", "..config..")
                .because("레이어 밖 클래스는 의존성 규칙 검사를 통째로 우회한다"));
```

**주의:** `freeze()`로 감싸므로 첫 실행이 현재 위반을 기준선으로 저장한다. 그 기준선 파일(`notification/src/test/archunit-violations/`)을 **커밋에 포함**해야 한다. Task 2·3이 진행되며 이 숫자가 줄어드는 것이 진행률이다.

**완료 조건:** `./gradlew :notification:test --tests '*HexagonalRulesTest*'` green, freeze 기준선 파일이 생성·커밋됨. 기준선 줄 수를 커밋 메시지에 기록.

---

## Task 2: infrastructure 재배치 (8개)

**목표:** 외부 기술 어댑터를 `infrastructure` 레이어로 옮긴다.

| 현재 | 이동 후 | 근거 |
|---|---|---|
| `redis/cache/RedisClientManager.java` | `sse/infrastructure/redis/RedisClientManager.java` | Redis 접속 관리. 사용처가 sse 중심(3:1) |
| `redis/publisher/RedisSseMessagePublisher.java` | `sse/infrastructure/redis/RedisSseMessagePublisher.java` | Redis pub 어댑터 |
| `redis/subscriber/RedisSseMessageSubscriber.java` | `sse/infrastructure/redis/RedisSseMessageSubscriber.java` | Redis sub 어댑터 |
| `redis/topic/RedisChannels.java` | `sse/infrastructure/redis/RedisChannels.java` | Redis 채널 상수 |
| `sse/cache/SseEmitterRepository.java` | `sse/infrastructure/SseEmitterRepository.java` | `SseEmitter`(Spring Web 타입)를 담는 인메모리 저장소 |
| `kafka/consumer/NotificationKafkaConsumer.java` | `notification/infrastructure/NotificationKafkaConsumer.java` | Kafka 인바운드 어댑터 |
| `notification/schedule/NotificationRetryScheduler.java` | `notification/infrastructure/NotificationRetryScheduler.java` | `@Scheduled` — 스프링 스케줄링 인프라 |
| `fcm/utils/FcmUtils.java` | `fcm/infrastructure/FcmUtils.java` | `com.google.firebase.messaging.*` 의존 |

**하는 일:** 파일 이동 + `package` 선언 수정 + 참조하는 모든 파일의 `import` 갱신(테스트 포함). **로직은 한 줄도 바꾸지 않는다.**

**완료 조건:** 4모듈 빌드 green. `git diff` 검토 시 이동·package·import 외 변경이 없어야 한다. Task 1이 만든 freeze 기준선이 줄어들었으면 스토어 갱신분을 같은 커밋에 포함.

---

## Task 3: application 재배치 (8개)

**목표:** 유스케이스 조율 코드를 `application` 레이어로 옮긴다.

| 현재 | 이동 후 |
|---|---|
| `notification/strategy/NotificationStrategy.java` | `notification/application/strategy/NotificationStrategy.java` |
| `notification/strategy/NotificationStrategyFactory.java` | `notification/application/strategy/NotificationStrategyFactory.java` |
| `notification/strategy/lecture/AcceptEnrollmentStrategy.java` | `notification/application/strategy/lecture/AcceptEnrollmentStrategy.java` |
| `notification/strategy/lecture/CancelEnrollmentStrategy.java` | `notification/application/strategy/lecture/CancelEnrollmentStrategy.java` |
| `notification/strategy/lecture/LectureUpdatedStrategy.java` | `notification/application/strategy/lecture/LectureUpdatedStrategy.java` |
| `notification/strategy/lecture/RejectEnrollmentStrategy.java` | `notification/application/strategy/lecture/RejectEnrollmentStrategy.java` |
| `notification/strategy/lecture/RequestEnrollmentStrategy.java` | `notification/application/strategy/lecture/RequestEnrollmentStrategy.java` |
| `notification/schedule/NotificationRetryService.java` | `notification/application/NotificationRetryService.java` |

**근거:** 전략은 "이벤트 종류에 따라 어떤 알림을 보낼지" 조율하는 유스케이스다. `NotificationRetryService`는 `@Transactional` 경계를 갖는 유스케이스다.

**하는 일:** Task 2와 동일 — 이동 + package + import 갱신. 로직 무변경.

**완료 조건:** 4모듈 빌드 green. freeze 기준선이 **0**이 되어야 한다(exception 3개는 규칙에서 제외했으므로). 0이 아니면 남은 항목을 보고할 것.

---

## Task 4: 전략 인터페이스 탈-Kafka (설계 변경)

**목표:** 유스케이스 인터페이스에서 인프라 DTO를 제거한다.

**현재 (결함):**
```java
public interface NotificationStrategy {
    default boolean supports(LectureEventType type) { return getSupportType() == type; }
    LectureEventType getSupportType();
    void handle(KafkaLectureEventMessage message);   // ← 인프라 DTO
}
```

`KafkaLectureEventMessage`와 `LectureEventType`은 `common.kafka` 패키지의 **전송 계층 타입**이다. 유스케이스가 이것에 직접 의존하면 (a) Kafka 메시지 스키마가 바뀔 때 전략 7개가 흔들리고 (b) 테스트가 Kafka DTO를 조립해야 한다.

**하는 일:**
1. `notification/application/strategy/`에 입력 커맨드 타입을 정의한다. 이름은 `NotificationCommand`(record 권장). 필드는 현재 전략들이 `KafkaLectureEventMessage`에서 **실제로 읽는 값만** 담는다 — 먼저 전략 5개를 읽어 사용 필드를 확인하고 그것만 옮길 것. 쓰지 않는 필드를 넣지 말 것(YAGNI).
2. 이벤트 종류 구분자도 `common.kafka.LectureEventType` 대신 application 소유 enum으로 둘지 판단한다. **판단 근거를 커밋 메시지에 남길 것** — 단순 재선언이 값을 못 하면 그대로 두는 것도 정당하다(과설계 회피). 다만 그 경우 `common.kafka` 의존이 남는다는 사실을 명시할 것.
3. `NotificationKafkaConsumer`(infrastructure)가 Kafka DTO → 커맨드 변환을 담당한다. 변환은 인바운드 어댑터의 책임이다.
4. 전략 7개의 시그니처를 새 커맨드로 바꾼다.

**완료 조건:**
- `notification/application/**`에서 `common.kafka` import가 0이거나, 남았다면 그 이유가 커밋 메시지에 근거와 함께 기록됨
- 4모듈 빌드 green
- 전략 하나 이상에 대해 **Kafka DTO 없이** 동작을 검증하는 테스트가 있을 것 — 이 리팩터의 목적이 그것이다

---

## 검증 (모든 Task 공통)

```bash
./gradlew :common:build :course:build :notification:build :app:build
```

freeze 잔여 확인:
```bash
find notification/src/test/archunit-violations -type f ! -name stored.rules -exec cat {} \; | grep -c .
```
