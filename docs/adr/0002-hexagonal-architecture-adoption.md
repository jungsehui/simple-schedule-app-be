# ADR-0002: 헥사고날(포트&어댑터) 아키텍처 채택과 래칫 기반 점진 마이그레이션

- 상태: 채택됨
- 날짜: 2026-07-04
- 관련: [ADR-0001](0001-multi-module-server-separation.md), [hexagonal-guidelines.md](../architecture/hexagonal-guidelines.md)

## 맥락

현재 코드는 바운디드 컨텍스트별 `presentation/application/domain` 레이어링(DDD 지향)을 갖추고 있으나,
외부 기술이 안쪽 계층에 침투해 있다:

- `fcm.domain.service.FcmMessageSender`가 `FirebaseMessaging`을 직접 보유 (도메인에 벤더 SDK)
- 도메인 엔티티가 JPA/Hibernate 애노테이션과 결합, 리포지토리는 Spring Data 인터페이스를 도메인 패키지에 위치
- `application` 계층이 `RestClient` 기반 `CourseClient`, `KafkaTemplate`, `RedissonClient`를 직접 사용
- `common.event.DomainEvent`는 이벤트 추상화 + JPA 엔티티(outbox) + 토픽 지정이 한 클래스에 결합

전면 재작성(빅뱅)은 235개 클래스·검증된 동시성 코드를 위험에 빠뜨리므로 배제한다.

## 결정

1. **목표 구조**: 컨텍스트별 `domain(순수 자바) / application(port.in·port.out + 유스케이스) /
   presentation(인바운드 어댑터) / infrastructure(아웃바운드 어댑터)`. 상세 규약은 hexagonal-guidelines.md.
2. **집행 메커니즘 = ArchUnit 래칫**: 목표 상태 규칙을 `FreezingArchRule`로 각 모듈에 상시 실행(CI 포함).
   - 현재 위반은 `src/test/archunit-violations/`에 **동결·커밋**(아키텍처 부채 대장)
   - **신규 위반은 즉시 테스트 실패** — 부채는 늘 수 없고 줄기만 한다
   - 스토어 라인 수 감소 = 마이그레이션 진행도 지표
3. **마이그레이션 스테이지** (각 스테이지는 빌드·테스트 그린 유지, 소단위 PR):
   - **Stage 0 (완료)**: 래칫 설치 — 규칙 3종(레이어 방향, application→presentation 금지, 도메인 순수성)
   - **Stage 1**: 외부 기술 포트화 — Firebase·RestClient·Redisson·KafkaTemplate 사용처를
     `application/port/out` 인터페이스 뒤로 이동, 구현은 `infrastructure/` 어댑터로.
     본보기: notification의 `EnrolledStudentsPort`(course 조회), `PushSenderPort`(FCM)
   - **Stage 2**: 리포지토리 포트화 — 도메인에 순수 리포지토리 인터페이스(포트), Spring Data
     인터페이스는 `infrastructure/persistence`로 이동해 포트를 구현(어댑터)
   - **Stage 3**: 도메인 모델/영속 모델 분리 여부 결정 — JPA 애노테이션 제거는 Member JOINED
     상속·연관관계 매핑 비용이 커서 **별도 ADR로 결정**. 그 전까지 도메인의 JPA 애노테이션은
     허용된 기술 부채(동결 상태)로 관리
   - **Stage 4**: `common` 분해 — `common.auth`(웹 인증)·`common.kafka/event`(메시징)·
     `common.domain`(영속 베이스)의 상호 독립을 규칙으로 강제(설치됨), 장기적으로 별도 모듈 분리
4. **DomainEvent 분리 방향**(Stage 2~3에서 실행): 순수 도메인 이벤트(불변 record) ↔ outbox
   영속 레코드(JPA) ↔ Kafka 메시지(계약)를 각기 다른 타입으로 두고 매퍼로 연결한다.

## 결과

- (+) 도메인·유스케이스가 순수해질수록 단위 테스트가 빠르고 안정적(EmbeddedKafka/H2 불필요 영역 확대)
- (+) 외부 기술 교체(FCM→다른 푸시, Redisson→다른 락)가 어댑터 교체로 국소화
- (+) 부채가 대장으로 가시화되고 래칫으로 단조 감소 보장
- (-) 포트/어댑터 간접층·매핑 코드 증가 — 1인 프로젝트에서 과설계 위험이 있으므로
  "외부 기술 경계에만 포트"를 원칙으로 하고 내부 호출에는 남용하지 않는다
- (-) Stage 3(영속 모델 분리)은 비용이 크므로 근거 없이 착수하지 않는다

## AI 에이전트 지침

- 새 코드는 hexagonal-guidelines.md 규약을 따를 것 (위반 시 ArchUnit이 차단)
- 아키텍처 위반을 해소한 경우 freeze 스토어 갱신분을 같은 커밋에 포함할 것
- 스토어에 라인을 **추가**해야 통과하는 변경은 설계 재검토 대상 — `ssa-architecture-guardian` 리뷰 필수
