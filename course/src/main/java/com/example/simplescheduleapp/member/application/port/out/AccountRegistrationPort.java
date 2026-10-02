package com.example.simplescheduleapp.member.application.port.out;

import com.example.simplescheduleapp.member.domain.Member;

/**
 * 아웃바운드 포트: 통합 신원 {@code account}에 SSA 회원을 등록한다 (ADR-0006 P1, 결정 1).
 *
 * <p>가입한 회원과 같은 트랜잭션에서 호출해야 한다. {@code account.id}는 {@code member_id}를 그대로
 * 쓰므로, 회원 저장이 롤백되면 account도 함께 롤백돼야 둘이 어긋나지 않는다.
 *
 * <p>GeekChat 쪽에 이미 같은 username의 account가 있으면 유니크 제약에 걸린다. 구현은 이를
 * {@code DUPLICATED_USERNAME_PHONE} 도메인 예외로 번역한다(회원 서브타입 저장과 같은 계약).
 */
public interface AccountRegistrationPort {

    void register(Member member);
}
