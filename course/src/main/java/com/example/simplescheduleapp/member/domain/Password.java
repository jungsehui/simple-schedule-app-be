package com.example.simplescheduleapp.member.domain;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 비밀번호 값 객체(VO) — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 시에는 {@code MemberEntity}가 해시 문자열을 그대로
 * {@code password} 컬럼에 보유하고, 매퍼가 {@code Password} ↔ {@code String}을 변환한다.
 * bcrypt 해시는 60자라 기존 컬럼(길이 미지정 = VARCHAR(255))에 그대로 들어간다 — 스키마 불변.
 *
 * <h2>해싱: bcrypt (구조 리뷰 HIGH 보안 수정)</h2>
 * 과거에는 <b>무염(salt 없는) SHA-256</b>을 썼다. 범용 다이제스트라 매우 빠르고 솔트가 없어
 * 레인보우 테이블·GPU 브루트포스에 취약하다. 비밀번호에는 의도적으로 느리고 솔트가 내장된
 * bcrypt를 쓴다. bcrypt 구현은 순수 자바 알고리즘 라이브러리이므로 도메인 순수성 규칙
 * (ArchUnit: spring/jakarta/hibernate 등 차단)에 위배되지 않는다.
 *
 * <h2>점진 마이그레이션 — 기존 사용자 무중단</h2>
 * 운영 DB에는 이미 레거시 SHA-256 해시가 저장돼 있어 알고리즘만 바꾸면 <b>전원 로그인 불가</b>가
 * 된다. 따라서 {@link #match}가 두 형식을 모두 검증하고, 레거시로 인증에 성공하면
 * {@link Member#login}이 그 자리에서 bcrypt로 승급한다({@link #isLegacy}). 형식 판별은 bcrypt의
 * {@code $2} 접두사로 한다(레거시는 64자 hex라 접두사가 없다).
 */
@Getter
public class Password {

    /** bcrypt 작업 인자 — 클수록 느려져 브루트포스 비용이 올라간다(라이브러리 기본값과 동일). */
    private static final int BCRYPT_COST = 10;
    private static final String BCRYPT_PREFIX = "$2";
    private static final String LEGACY_ALGORITHM = "SHA-256";

    private final String hashedPassword;

    public Password(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    /** 신규 가입·승급용 — 항상 bcrypt로 해싱한다. */
    public static Password hashPassword(String password) {
        return new Password(BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray()));
    }

    /**
     * 평문과 대조한다. 저장된 해시가 레거시(무염 SHA-256)여도 인증에 성공해야 하므로
     * 두 형식을 모두 지원한다 — 점진 마이그레이션의 핵심.
     */
    public boolean match(String password) {
        if (isLegacy()) {
            // 타이밍 공격 회피를 위한 상수 시간 비교
            return MessageDigest.isEqual(
                    legacyHash(password).getBytes(StandardCharsets.UTF_8),
                    hashedPassword.getBytes(StandardCharsets.UTF_8));
        }
        return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified;
    }

    /**
     * 레거시(무염 SHA-256) 해시인가 — 로그인 성공 시 bcrypt 승급 대상임을 뜻한다.
     * bcrypt 해시는 항상 {@code $2} 접두사를 가진다.
     */
    public boolean isLegacy() {
        return hashedPassword != null && !hashedPassword.startsWith(BCRYPT_PREFIX);
    }

    /** 레거시 검증 전용 — 신규 해싱에는 쓰지 않는다(전원 승급 완료 후 제거 가능). */
    private static String legacyHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(LEGACY_ALGORITHM);
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(LEGACY_ALGORITHM + " 암호화 중 오류 발생", e);
        }
    }
}
