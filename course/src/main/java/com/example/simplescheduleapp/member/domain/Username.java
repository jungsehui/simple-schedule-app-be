package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 로그인 식별자. <b>규칙의 단일 원천이다.</b> 요청 DTO의 {@code @Pattern}은 이 규칙을 미러링해야
 * 한다(실제로 동기화돼 있는지는 DTO 쪽 코드에서 확인한다. 이 클래스는 그 배선 자체를 보장하지 않는다).
 *
 * <p><b>왜 값 객체인가.</b> 규칙이 요청 DTO에만 있으면 DTO를 거치지 않는 경로(내부 호출,
 * 테스트, 향후 account 백필)가 규칙을 우회한다. 정규화도 마찬가지다 — 한 군데서만 하면
 * 다른 경로로 들어온 대문자가 그대로 저장돼 {@code Admin}과 {@code admin}이 공존한다.
 *
 * <p><b>규칙 도출(2026-08-26 통합 결정).</b> 두 시스템의 정규식 중 하나를 고른 것이 아니라
 * 각각의 안전 속성을 취했다:
 * <ul>
 *   <li><b>소문자만</b>(geekchat) — 대소문자 변형은 {@code Admin}/{@code admin} 사칭만 만들고 이득이 없다</li>
 *   <li><b>영문자 1자 이상</b>(SSA) — 전부 숫자면 {@code memberId}와 혼동된다</li>
 *   <li><b>밑줄 허용·최소 3자</b>(geekchat) — 넓은 쪽. 4자를 요구할 안전 근거가 없다</li>
 * </ul>
 *
 * <p>좁히는 변경은 파괴적이고 넓히는 변경은 추가형이라, 좁은 쪽에서 시작한다.
 */
public record Username(String value) {

    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-z])[a-z0-9_]{3,20}$");

    /**
     * 불변식: value는 항상 정규화된 상태로 규칙을 만족한다.
     *
     * <p>record의 정준 생성자는 record보다 좁게 만들 수 없어 {@code new Username(...)}을
     * 막을 수 없다. 그래서 여기서 검증한다 — {@code of()}를 거치지 않은 생성도 규칙을
     * 통과해야 한다. 이 클래스의 존재 이유가 "of()를 우회하는 경로 차단"이므로 이 구멍을
     * 열어 두면 클래스가 자기 목적을 배반한다.
     */
    public Username {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_FORMAT);
        }
    }

    /**
     * 검증하고 정규화한다. <b>정규화가 검증보다 먼저다</b> — 대문자를 먼저 거부하면
     * "대문자 입력은 소문자로 받아 준다"는 규칙이 성립하지 않는다.
     *
     * <p>{@link Locale#ROOT}로 소문자화한다 — JVM 기본 로케일에 맡기면 튀르키예어(tr) 로케일에서
     * {@code "ADMIN".toLowerCase()}가 {@code "admın"}(dotless i, U+0131)이 되어 배포 환경에 따라
     * 같은 입력의 통과 여부가 갈린다.
     */
    public static Username of(String raw) {
        if (raw == null) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_FORMAT);
        }
        return new Username(raw.toLowerCase(Locale.ROOT));
    }
}
