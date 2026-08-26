package com.example.simplescheduleapp.member.domain;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * {@link Username}의 정준(canonical) 생성자를 프로덕션 코드가 직접 호출하지 못하게 막는다.
 *
 * <p><b>왜 필요한가.</b> 정규화(대문자 → 소문자)는 {@link Username#of(String)}에만 있다.
 * 정준 생성자는 record라서 {@code private}으로 좁힐 수 없어 불변식만 지킨다({@code Username}의
 * javadoc 참고). 그래서 향후 Jackson 역직렬화, JPA 컨버터, 다른 팩토리 메서드 등 {@code of()}를
 * 거치지 않고 {@code new Username(...)}을 직접 부르는 경로가 생기면, 정규화 없이 이미 소문자인
 * 값만 통과하는 셈이 돼 "대문자 입력은 거부가 아니라 정규화한다"는 규칙이 조용히 깨진다(대문자
 * 입력은 정준 생성자의 검증에 걸려 즉시 400이 난다. 규칙이 깨지는 게 아니라 사용자 경험이
 * 깨진다). 이 규칙은 그 우회 경로 자체를 기계로 차단한다.
 *
 * <p><b>{@code freeze()}로 감싸지 않는다.</b> 이 규칙은 위반이 0건이어야 정상이고, 새 위반은
 * 즉시 테스트 실패로 드러나야 한다. {@code freeze()}는 기존 위반을 동결해 통과시키는
 * 장치라 이 규칙의 목적(신규 우회 즉시 차단)과 맞지 않는다. freeze 스토어도 만들지 않는다.
 *
 * <p><b>스캔 범위를 프로덕션 코드로 한정한다.</b> {@code UsernameTest}의
 * {@code 정준_생성자도_규칙을_검사한다}가 의도적으로 {@code new Username(...)}을 직접 호출하므로,
 * {@link ImportOption.DoNotIncludeTests}로 테스트 클래스를 스캔에서 뺀다. 이 규칙이 막는 것은
 * "프로덕션 코드의 우회"이지 "생성자가 검증한다는 것을 확인하는 테스트"가 아니다.
 */
@AnalyzeClasses(packages = "com.example.simplescheduleapp",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class UsernameConstructionRuleTest {

    @ArchTest
    static final ArchRule only_username_may_call_its_own_canonical_constructor =
            noClasses().that().areNotAssignableTo(Username.class)
                    .should().callConstructor(Username.class, String.class)
                    .because("Username의 정규화는 of()에만 있다. 정준 생성자를 직접 호출하는 "
                            + "경로가 생기면 of()를 우회해 정규화 없이 값이 만들어진다");
}
