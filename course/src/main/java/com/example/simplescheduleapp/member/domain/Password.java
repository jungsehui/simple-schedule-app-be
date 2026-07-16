package com.example.simplescheduleapp.member.domain;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 비밀번호 값 객체(VO) — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 영속 시에는 {@code MemberEntity}가 해시 문자열을 그대로
 * {@code password} 컬럼에 보유하고, 매퍼가 {@code Password} ↔ {@code String}을 변환한다
 * (컬럼은 동일 — 스키마 불변). 해시 알고리즘·동작은 순수화 전과 같다.
 */
@Getter
public class Password {

    private static final String ALGORITHM = "SHA-256";

    private final String hashedPassword;

    public Password(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public static Password hashPassword(String password) {
        return new Password(hash(password));
    }

    public boolean match(String password) {
        return this.hashedPassword.equals(hash(password));
    }

    private static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ALGORITHM + " 암호화 중 오류 발생", e);
        }
    }
}
