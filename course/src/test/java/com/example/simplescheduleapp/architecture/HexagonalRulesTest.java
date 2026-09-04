package com.example.simplescheduleapp.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * course 모듈 헥사고날 아키텍처 규칙 (docs/adr/0002 참고).
 *
 * <p>모든 규칙은 {@code freeze(...)}로 감싼다: 현재 존재하는 위반은
 * {@code src/test/archunit-violations/}에 동결(커밋)되어 있고, 리팩토링으로 줄어들면
 * 스토어가 함께 줄어든다(래칫). <b>신규 위반은 즉시 테스트 실패</b>로 차단된다.
 *
 * <p>common 의존성은 jar로 인식되므로 {@code DoNotIncludeJars}에 의해 스캔에서 제외된다
 * (common 자체 규칙은 common 모듈의 동일 테스트가 담당).
 */
@AnalyzeClasses(packages = "com.example.simplescheduleapp",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class HexagonalRulesTest {

    // 1. 레이어 방향: domain은 application/presentation/config를 알지 못한다
    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers = freeze(
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..presentation..", "..config..")
                    .because("도메인은 유스케이스·웹·설정 계층을 몰라야 한다 (의존성 역전)"));

    // 2. application은 presentation을 알지 못한다 (요청/응답 DTO는 presentation 소유)
    @ArchTest
    static final ArchRule application_should_not_depend_on_presentation = freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..")
                    .because("유스케이스 계층은 전달 메커니즘(웹)과 독립적이어야 한다"));

    // 3. 도메인 순수성(목표 상태): 프레임워크·외부 기술이 도메인에 침투하지 않는다.
    //    현재의 JPA/Spring-Data 위반은 freeze 스토어에 동결됨 — 신규 침투만 차단.
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
     * 4. 모든 프로덕션 클래스는 레이어 패키지 안에 있어야 한다.
     *
     * <p>위 1~3번 규칙은 전부 {@code ..domain..}이나 {@code ..application..}을 기점으로 삼는다.
     * 그래서 레이어 밖에 있는 클래스는 검사 대상이 아예 아니었다. 규칙이 통과한다는 것이
     * 구조가 깨끗하다는 뜻이 아니었던 셈이다. 이 규칙이 그 사각지대를 닫는다.
     *
     * <p><b>기준선 8건</b>({@code event}의 이벤트 5개, {@code event.mapper}, {@code event.relay},
     * {@code redis.lock}). 세 방법으로 교차 검산했다: 소스 파일 열거 8개, 제외 없이 돌린 ArchUnit
     * 실측 15건에서 exception 7건을 뺀 8건, 컴파일된 {@code .class} 8개(익명 클래스 없음).
     *
     * <p>{@code ..exception..} 제외는 notification과 같은 관행이다. 프로젝트 전반이 도메인별
     * {@code exception} 패키지에 예외 코드를 두므로 의도적으로 허용한다(제외하지 않으면 기준선 15건).
     * notification이 함께 제외한 {@code ..architecture..}는 course에서는 넣지 않았다.
     * {@code DoNotIncludeTests}가 이미 테스트 패키지를 전부 걷어내는 것을 실측으로 확인했고
     * (제외 없이 돌린 15건에 아키텍처 테스트가 하나도 없었다), 효과 없는 제외는 나중에 그 패키지에
     * 프로덕션 클래스가 들어오면 조용히 뚫리는 구멍이 되기 때문이다.
     */
    @ArchTest
    static final ArchRule classes_should_live_in_a_layer = freeze(
            classes().that().resideOutsideOfPackages("..exception..")
                    .should().resideInAnyPackage("..domain..", "..application..", "..infrastructure..", "..presentation..", "..config..")
                    .because("레이어 밖 클래스는 의존성 규칙 검사를 통째로 우회한다"));

    /**
     * 5. application은 infrastructure를 알지 못한다 (구현 세부는 포트를 통해 역전).
     *
     * <p><b>기준선 0건.</b> course의 영속성은 이미 포트와 어댑터로 갈라져 있다. 리포지토리 인터페이스는
     * {@code domain}에, JPA 어댑터는 {@code infrastructure}에 있고, {@code infrastructure}를 import 하는
     * 클래스는 전부 자기도 {@code infrastructure} 안에 있다(손검산으로 확인). 0은 부채가 없다는 뜻이지
     * 규칙이 헛돈다는 뜻이 아니며, 위반을 주입해 빨간불이 나는 것으로 반증했다.
     * (notification의 같은 규칙은 FcmService 관련 5건이 의도적으로 동결돼 있다.)
     */
    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure = freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("유스케이스는 포트에만 의존한다 — 구현 교체가 코어를 흔들면 안 된다"));
}
