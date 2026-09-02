package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.member.domain.Username;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public LoginResult login(String username, String password) {
        Member member = findForLogin(username);

        // 레거시(무염 SHA-256) 해시로 로그인에 성공하면 도메인이 bcrypt로 승급한다.
        // 승급된 경우에만 저장해 기존 사용자를 끊지 않고 점진 마이그레이션한다. (ADR-0004)
        boolean passwordUpgraded = member.login(password);
        if (passwordUpgraded) {
            memberRepository.save(member);
        }

        return new LoginResult(member.getId(), member.getRole());
    }

    /**
     * 저장된 값을 <b>그대로 먼저</b> 찾고, 없을 때만 정규화한 값으로 한 번 더 찾는다.
     *
     * <p><b>왜 두 번 찾는가.</b> DB에 두 세대의 username이 섞여 있다. 규칙 통일 전에 가입한
     * 사용자는 친 그대로 저장돼 있어 {@code JungSehui}일 수 있고, 이후 가입자는 소문자로
     * 정규화돼 저장된다. 운영 PostgreSQL은 {@code =} 비교가 대소문자를 구분하므로
     * <b>한 가지 조회 전략만으로는 반드시 한쪽이 잠긴다.</b>
     * <ul>
     *   <li>원시 입력만 쓰면: {@code Abc12}로 가입한 신규 사용자가 {@code abc12}로 저장돼
     *       자기가 친 아이디로 못 들어온다</li>
     *   <li>정규화만 쓰면: {@code JungSehui}로 저장된 기존 사용자 전원이 못 들어온다</li>
     * </ul>
     *
     * <p><b>정확 일치가 먼저여야 하는 이유.</b> 순서를 뒤집어 정규화를 먼저 두면 조회 키가
     * 항상 정규형이 되어, {@code JungSehui}처럼 저장된 구세대 행은 <b>어떤 입력으로도 도달할
     * 수 없다.</b> 이게 기본 결과다. 여기에 더해 {@code Foo}(구세대)와 {@code foo}(신세대)가
     * 공존하고 비밀번호까지 겹치면 구세대 사용자가 <b>남의 계정으로 로그인</b>한다. 이게 최악의
     * 결과다. 정확 일치를 먼저 두면 두 계정 모두 자기가 친 문자열로만 도달한다.
     *
     * <p><b>이 분기는 한시적이다.</b> 저장된 username을 전부 소문자로 백필하고 유니크 제약을
     * 대소문자 무시로 바꾸면(ADR-0003 Stage 5 후속) 정규화 한 번으로 끝난다. 그 전까지
     * 지우면 둘 중 한 세대가 잠긴다.
     *
     * <p>정규화에 {@link Username#of}가 아니라 {@link Username#normalize}를 쓰는 이유: 옛
     * 규칙으로 저장된 값은 새 규칙을 만족하지 않을 수 있는데, {@code of()}는 그런 입력에
     * 인증 실패가 아니라 형식 오류를 던진다.
     */
    private Member findForLogin(String username) {
        return memberRepository.findByUsername(username)
                .or(() -> {
                    String normalized = Username.normalize(username);
                    // 이미 정규형이면 같은 질의를 두 번 던지는 셈이라 건너뛴다.
                    if (normalized == null || normalized.equals(username)) {
                        return Optional.empty();
                    }
                    return memberRepository.findByUsername(normalized);
                })
                .orElseThrow(() -> new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD));
    }
}
