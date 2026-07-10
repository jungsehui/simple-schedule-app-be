package com.example.simplescheduleapp.common.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * common 모듈 헥사고날 아키텍처 규칙 (docs/adr/0002 참고).
 *
 * <p>common은 공유 커널이므로 domain(common.domain)과 event 추상화가 특히 순수해야 한다.
 * 현재 위반(예: DomainEvent가 JPA 엔티티)은 {@code src/test/archunit-violations/}에 동결 —
 * 리팩토링으로 줄어들면 스토어가 함께 줄어들고, <b>신규 위반은 즉시 실패</b>한다.
 *
 * <p>주의: common은 java-test-fixtures 플러그인 때문에 자기 main 클래스가 jar로 테스트
 * 클래스패스에 올라간다 — DoNotIncludeJars를 쓰면 아무 클래스도 스캔되지 않으므로 제외.
 * 범위 제한은 packages 필터가 담당한다.
 */
@AnalyzeClasses(packages = "com.example.simplescheduleapp.common",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalRulesTest {

    // 1. 도메인 순수성(목표 상태): common.domain / common.event는 순수 자바를 지향한다
    @ArchTest
    static final ArchRule domain_should_be_framework_free = freeze(
            noClasses().that().resideInAnyPackage("..common.domain..", "..common.event..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "com.google.firebase..",
                            "org.redisson..",
                            "org.apache.kafka..",
                            "io.jsonwebtoken..",
                            "com.fasterxml.jackson..")
                    .because("공유 커널의 도메인·이벤트 추상화는 순수 자바여야 한다"));

    // 2. auth(웹 인증 인프라)는 kafka/event(메시징)와 서로 침투하지 않는다 — 향후 모듈 분리 대비
    @ArchTest
    static final ArchRule auth_and_messaging_are_independent = freeze(
            noClasses().that().resideInAPackage("..common.auth..")
                    .should().dependOnClassesThat().resideInAnyPackage("..common.kafka..", "..common.event..")
                    .because("인증과 메시징은 독립 관심사 — common 분리(ADR-0002)의 전제"));
}
