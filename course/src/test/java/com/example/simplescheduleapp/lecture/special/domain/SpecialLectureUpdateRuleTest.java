package com.example.simplescheduleapp.lecture.special.domain;

import com.example.simplescheduleapp.schedule.domain.Schedule;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * {@link SpecialLecture#update}를 프로덕션 코드가 호출하지 못하게 막는다.
 *
 * <p><b>왜 필요한가.</b> 특강 신청의 마감 검사는 {@code endTime}을 Redis에 캐시해 두고
 * Lua가 읽는다. DB를 치지 않고 거절하기 위해서다(1차 방어선의 존재 이유). 그 설계는
 * <b>{@code endTime}이 생성 후 바뀌지 않는다</b>는 전제 위에 서 있다.
 *
 * <p>그런데 그 전제는 구조적 보장이 아니라 <b>우연</b>이다. {@code update}는 살아 있고
 * 내부에서 {@code updateSchedule}로 시작·종료 시각을 바꾼다. 지금 호출자가 없을 뿐이다.
 * 특강 수정 API를 붙이는 사람이 가장 먼저 부를 이름이고, 그 순간 Redis의 마감 사본이
 * 조용히 낡는다. <b>아무것도 실패하지 않고 신청 가능 기간만 틀려진다</b> — 알아차릴
 * 방법이 없는 종류의 고장이다.
 *
 * <p>그래서 삭제가 아니라 차단이다. {@code update}는 결함이 아니라 아직 배선되지 않은
 * 기능이다. 배선하려는 사람이 이 규칙에 걸려 <b>Redis 마감 키 갱신을 함께 처리하도록</b>
 * 강제하는 것이 목적이다. 그때 이 규칙을 지우는 것이 올바른 해소다.
 *
 * <p>위반 0건이 정상이므로 {@code freeze()}를 쓰지 않는다. 스캔은
 * {@link ImportOption.DoNotIncludeTests}로 프로덕션 코드에 한정한다 — 도메인 테스트는
 * {@code update}를 직접 호출해 동작을 검증할 수 있어야 한다.
 */
@AnalyzeClasses(packages = "com.example.simplescheduleapp",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class SpecialLectureUpdateRuleTest {

    @ArchTest
    static final ArchRule special_lecture_update_must_not_be_called_from_production =
            noClasses().that().areNotAssignableTo(SpecialLecture.class)
                    .should().callMethod(SpecialLecture.class, "update", Long.class, Schedule.class, int.class)
                    .because("특강 신청 마감 검사가 endTime의 Redis 사본을 읽는다. update로 시각을 "
                            + "바꾸면 그 사본이 낡는데 아무 검사도 실패하지 않는다. 이 메서드를 "
                            + "배선하려면 Redis 마감 키 갱신을 함께 처리해야 한다");
}
