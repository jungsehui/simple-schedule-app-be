package com.example.simplescheduleapp.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * notification 모듈 헥사고날 아키텍처 규칙 (docs/adr/0002 참고).
 *
 * <p>모든 규칙은 {@code freeze(...)}로 감싼다: 현재 존재하는 위반은
 * {@code src/test/archunit-violations/}에 동결(커밋)되어 있고, 리팩토링으로 줄어들면
 * 스토어가 함께 줄어든다(래칫). <b>신규 위반은 즉시 테스트 실패</b>로 차단된다.
 */
@AnalyzeClasses(packages = "com.example.simplescheduleapp",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class HexagonalRulesTest {

    // 1. 레이어 방향: domain은 application/presentation/infrastructure/config를 알지 못한다
    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers = freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..presentation..", "..infrastructure..", "..config..")
                    .because("도메인은 유스케이스·웹·인프라·설정 계층을 몰라야 한다 (의존성 역전)"));

    // 2. application은 presentation을 알지 못한다 (요청/응답 DTO는 presentation 소유)
    @ArchTest
    static final ArchRule application_should_not_depend_on_presentation = freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..")
                    .because("유스케이스 계층은 전달 메커니즘(웹)과 독립적이어야 한다"));

    // 2-1. application은 infrastructure를 알지 못한다 (구현 세부는 포트를 통해 역전)
    //      기존 위반(예: FcmService의 FcmMessageSender)은 freeze 스토어에 동결 — 신규 결합만 차단.
    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure = freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("유스케이스 계층은 구현 세부(인프라)와 독립적이어야 한다 (의존성 역전)"));

    // 3. 도메인 순수성(목표 상태): 프레임워크·외부 기술이 도메인에 침투하지 않는다.
    //    현재 위반(예: fcm.domain.service의 FirebaseMessaging)은 freeze 스토어에 동결 — 신규 침투만 차단.
    @ArchTest
    static final ArchRule domain_should_be_framework_free = freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "com.google.firebase..",
                            "org.redisson..",
                            "org.apache.kafka..",
                            "io.jsonwebtoken..",
                            "com.fasterxml.jackson..")
                    .because("도메인 코어는 순수 자바여야 한다 — 외부 기술은 어댑터에서만"));

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
}
