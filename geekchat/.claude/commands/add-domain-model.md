---
description: 새 도메인 모델 + JPA 엔티티 + 라운드트립 테스트 추가
argument-hint: <ModelName> [field1:Type field2:Type? ...]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

새 도메인 모델을 추가한다. 헥사고날 분리(domain ↔ JPA)를 따른다.

## 절차

1. **도메인 모델** — `src/main/kotlin/com/geekchat/server/domain/model/<ModelName>.kt`
   - 순수 Kotlin `data class` + `val`
   - Spring/JPA import 절대 금지
   - 검증/변경은 `with*()` 메서드로
   - companion object에 정규식/상수 (필요 시)

2. **JPA 엔티티** — `src/main/kotlin/com/geekchat/server/adapter/out/persistence/entity/<ModelName>JpaEntity.kt`
   - `class` + `var` (Hibernate proxy 호환)
   - 적절한 base class 상속 (`BaseJpaEntity` 또는 `SoftDeletableJpaEntity`)
   - 모든 컬럼 round-trip 매핑 (`toDomain()` + `fromDomain()`)
   - `@Table(name = "...")` (MySQL 호환 이름, 필요 시 indexes/uniqueConstraints)

3. **포트 인터페이스** — `src/main/kotlin/com/geekchat/server/application/port/out/<ModelName>Repository.kt`
   - findById, save 등 기본 CRUD

4. **Spring Data 인터페이스** — `src/main/kotlin/com/geekchat/server/adapter/out/persistence/repository/SpringData<ModelName>Repository.kt`
   - `JpaRepository<XJpaEntity, String>` 상속

5. **포트 어댑터** — `src/main/kotlin/com/geekchat/server/adapter/out/persistence/adapter/<ModelName>RepositoryAdapter.kt`
   - 포트 구현. `@Repository`. 매핑 호출.

6. **라운드트립 테스트** — `src/test/.../integration/RoundTripMappingTest.kt`에 새 테스트 추가:
   ```kotlin
   @Test
   fun `XJpaEntity round-trip preserves all fields`() {
       val entity = XJpaEntity(...)
       val saved = repo.saveAndFlush(entity)
       val loaded = repo.findById(saved.id).get()
       val domain = loaded.toDomain()
       assertEquals(entity.id, domain.id)
       // 모든 필드 검증
   }
   ```

7. **도메인 단위 테스트** — `src/test/.../domain/model/DomainModelTest.kt`에 invariant/메서드 테스트 추가.

8. **문서 업데이트** — `docs/DATABASE.md`에 새 테이블 SQL 스키마 추가, `AGENTS.md` 도메인 용어집에 추가.

## 체크리스트
- [ ] 도메인에 JPA import 0개 (`.claude/hooks/check-domain-imports.sh`로 자동 검증)
- [ ] toDomain ↔ fromDomain 모든 필드 매핑
- [ ] RoundTripMappingTest 통과
- [ ] `./gradlew test` 통과
