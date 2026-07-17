# ADR-0004: 도메인 모델 / 영속 모델 분리 — 완전 순수 도메인화

- 상태: 채택됨
- 날짜: 2026-07-16
- 관련: [ADR-0002](0002-hexagonal-architecture-adoption.md)(Stage 3 결정 지점), [ADR-0003](0003-modular-monolith-consolidation.md), [hexagonal-guidelines.md](../architecture/hexagonal-guidelines.md)
- 대체: ADR-0002의 "Stage 3: 도메인 모델/영속 모델 분리 여부 결정 — 별도 ADR" 를 이 문서로 확정

## 맥락

ADR-0002는 헥사고날 마이그레이션을 래칫 기반으로 진행하며 **Stage 2까지 완료**했다:
- 외부 기술(Firebase·RestClient·Redisson·KafkaTemplate)은 `application/port/out` 뒤로 이동(Stage 1)
- 리포지토리는 도메인 소유 순수 포트 + `infrastructure/persistence` 어댑터로 분리(Stage 2)

그러나 **도메인 엔티티 자체는 여전히 JPA `@Entity`** 이다. 구조 리뷰(11개 바운디드 컨텍스트, 2026-07-16)로 실측한 현황:
- 도메인 파일 36개 중 **21개(58%)가 `jakarta.persistence`/`org.hibernate`/`org.springframework` 누수**
- `Member`(JOINED 상속 루트)·`Schedule`(JOINED 상속 루트)·`Password`(@Embeddable)·`FcmToken`·`FailedNotification` 등이 도메인 행위와 저장 row를 겸임(변경증폭)
- 순수 포트가 JPA 엔티티를 반환하므로 **저장기술 독립성은 실제로 미확보**(포트는 Spring Data만 가리고 JPA는 못 가림)

이 누수는 ADR-0002가 **"허용된 기술 부채(동결)"** 로 지정하고 `HexagonalRulesTest.domain_should_be_framework_free` freeze 스토어에 동결한 상태다. ADR-0002는 Stage 3(영속 모델 분리)를 "비용이 크므로 **근거 없이 착수하지 않는다**"(결과 §49)며 별도 ADR로 미뤘다.

## 결정

**전 바운디드 컨텍스트의 도메인 모델을 완전 순수 Java화한다.** 표준 패턴:

```
domain/                  순수 Java — 프레임워크/ORM import 0, 불변식·행위 보유
  <Aggregate>.java       (POJO, jakarta/hibernate/spring 없음)
  <ValueObject>.java
  <Aggregate>Repository.java   (순수 포트 — 이미 존재, ADR-0002 Stage 2)
infrastructure/persistence/
  <Aggregate>Entity.java       (JPA @Entity — 매핑·@SQLDelete·@Version·상속만 담당)
  <Aggregate>Mapper.java       (도메인 ↔ 엔티티 변환)
  <Aggregate>JpaRepository.java (Spring Data)
  <Aggregate>RepositoryAdapter.java (포트 구현 — 엔티티↔도메인 매핑해 순수 도메인 반환)
```

- 도메인은 **인프라를 전부 삭제해도 컴파일**돼야 한다(순수성 최종 기준).
- `DomainEvent`도 ADR-0002 §4대로 순수 도메인 이벤트(불변 record) ↔ outbox 영속 레코드(JPA) ↔ Kafka 메시지(계약)를 분리한다.

### 근거 (ADR-0002 §49 "근거 없이 착수 안 함"에 대한 응답)

사용자(프로젝트 오너)가 다음을 근거로 완전 순수화를 명시 결정(2026-07-16):
1. geek-chat-server-v2 수준의 Clean + Hexagonal 아키텍처 학습·정합성 목표
2. 외부 기술 의존도 완전 추상화 → 저장기술·프레임워크 교체 비용 국소화
3. SOLID·DDD·순수 Rich 도메인 원칙의 일관 적용

### 집행 = ArchUnit 래칫 (ADR-0002 계승)

- 컨텍스트를 순수화할 때마다 `domain_should_be_framework_free` freeze 스토어에서 해당 라인을 **제거**(부채 단조 감소).
- 스토어 라인 수 = 진행도 지표. 전 컨텍스트 완료 시 스토어 0 → 규칙을 freeze 해제(엄격 모드)로 승격.
- 신규 도메인 누수는 즉시 CI 실패.

> **진행 상태** (2026-07-17): **Phase A 완료** — course 전 애그리게잇의 교차 참조를 ID로 전환
> (Lecture/SpecialLecture/Consultation→Tutor, LectureEnrollment·SpecialLectureEnrollment,
> ConsultationAttendee→Parent). `ConsultationAttendee.consultation`만 애그리게잇 내부 합성으로 유지.
> **Phase B 대부분 완료** — notification 전체(FailedNotification·FcmToken), **Member JOINED 계층**
> (다형 매퍼), **Schedule JOINED 계층**(@Version 낙관락 왕복 — `LectureOptimisticLockTest`로 실증),
> 수강등록 3종, 도메인 서비스 @Component 제거.
> **ArchUnit freeze 스토어: course 148줄 → 0줄, notification 0줄**(신규 부채 0) = course·notification
> 도메인 100% 프레임워크-프리. 전 모듈 106 테스트 green(`--rerun-tasks` 강제 재실행).
> **남은 것**: common 54줄(공유 커널·DomainEvent), Password bcrypt(리뷰 HIGH), sse 자원누수,
> notification↔fcm 순환.

### 마이그레이션 순서 (각 단계 = 빌드·테스트 그린, 개별 가역, 컨텍스트 단위 커밋)

1. **Leaf 단일 엔티티**(상속 무관, 패턴 정립): `notification/FailedNotification`, `fcm/FcmToken`
2. **VO 선분리**: `member/Password`(상속 무관) + **무염 SHA-256 → bcrypt 보안 결함 동시 수정**
3. **DomainEvent 3분리**(common): 순수 record ↔ outbox JPA ↔ Kafka 메시지
4. **JOINED 상속 루트**(blast radius 최대, 최후): `Schedule`→Lecture/SpecialLecture/Consultation, `Member`→Student/Tutor/Parent — 순수 도메인 + JOINED 매핑 담당 JPA 엔티티 + 서브타입별 매퍼
5. **`common.domain`→`common.persistence` 리네임**: 영속 베이스(BaseDomain/SoftDeletedDomain)는 @MappedSuperclass 유지하되 "domain" 오해 제거

## 결과

- (+) 도메인 단위 테스트가 인프라 없이 실행(H2/EmbeddedKafka 불필요 영역 확대)
- (+) 저장기술 교체가 어댑터/매퍼 교체로 완전 국소화
- (+) 순수 Rich 도메인 — 불변식이 프레임워크와 무관하게 표현·검증
- (−) **매퍼·이중 모델 유지비 증가** — 구조 리뷰가 대부분 컨텍스트에서 이를 over-engineering으로 판정했음을 명시 기록(아래 기각 대안 B). 애그리게잇이 얇은 CRUD 컨텍스트일수록 매핑 코드가 순비용.
- (−) JOINED 상속 계층 순수화는 회귀 위험 large — 반드시 최후, 소단위, 테스트 우선.

## 기각한 대안

- **A. ADR-0002 현상 유지(동결 지속)** — 도메인 JPA 애노테이션을 허용 부채로 계속 동결. 기각: 오너가 완전 순수화를 결정.
- **B. 타깃 고가치 수정만**(구조 리뷰 권장) — Password bcrypt, fcm SDK 포트, sse 자원 누수, notification↔fcm 순환만 고치고 순수화는 미착수. 리뷰 근거: SSA 불변식 복잡도(돈/상태머신 없음)에 비해 전면 순수화는 대부분 컨텍스트에서 over-engineering. **기각하되, 이 안의 고가치 수정 항목은 ADR-0004 내에서 함께 실행**한다.
- **C. member+special-lecture만 순수화** — 복잡 후보 2개만. 기각: 오너가 전면 결정. (단 실행 순서상 이 둘은 JOINED 상속이라 최후에 배치.)

## 롤백

- 컨텍스트 단위 커밋이므로 문제 발생 시 해당 컨텍스트 커밋만 `git revert`.
- freeze 스토어는 컨텍스트가 완전 이관될 때만 라인 제거 → 미완 상태로 규칙이 깨지지 않음.
- JOINED 상속(4단계) 착수 전, 그 앞 단계(leaf·VO·event)가 모두 그린·병합된 상태를 전제.

## AI 에이전트 지침

- 컨텍스트 하나를 끝내기 전 다음으로 넘어가지 말 것(부분 순수화 = 이중 유지 최악).
- 도메인 순수화 커밋은 freeze 스토어 갱신분을 **같은 커밋에** 포함(ADR-0002 §54 계승).
- 각 컨텍스트 완료 후 `ssa-architecture-guardian` + `ssa-qa-reviewer` 이중 검증.
