# common 모듈

course·notification이 공유하는 **라이브러리 모듈** (bootJar 비활성, `jar`만 생성).

## 포함 내용
- `common.event` — `DomainEvent`(단일 테이블 상속 엔티티, uuid/status/targetDomainId, soft delete), `EventStatus`
- `common.kafka` — `KafkaLectureEventMessage`(record), `LectureEventType`, `topic.KafkaTopics`(토픽 상수)
- JWT 인증 (jjwt 0.12.5) — `token.secretKey`, `accessTokenExpirationMillis` 설정 필요
- 공통 도메인 베이스 (`SoftDeletedDomain` 등), 공통 예외
- `testFixtures/` — 하위 모듈 테스트가 쓰는 픽스처: `@SpringBootTest` 지원, `@EmbeddedKafka` 지원, Fixture Monkey 1.1.11

## 규칙
- **특정 서버 전용 로직 유입 금지** — course/notification 한쪽만 쓰는 코드는 해당 모듈에
- 여기의 public API 변경은 모든 하위 모듈 재컴파일·테스트 필요: `./gradlew build`
- Kafka 메시지 스키마(`KafkaLectureEventMessage`) 변경은 course(생산자)·notification(소비자) 양쪽 호환성 확인 필수
- 의존성 전파: 하위 모듈에 노출할 라이브러리만 `api`, 내부 구현은 `implementation`

## 주의 (이력)
- `DomainEvent.toMessage()` 추상 메서드는 제거됨 — 이벤트→메시지 변환은 생산자 측 책임으로 이동. 남아 있는 추상 메서드는 `getTopic()`뿐.
