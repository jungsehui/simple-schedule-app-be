package com.example.simplescheduleapp.member.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Embeddable
public class Password {

    private static final String ALGORITHM = "SHA-256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 1024;

    private String salt;
    private String hash;

    @Column(name = "password", nullable = false)
    private String hashedPassword;

    private Password(String hashedPassword) {
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
            throw new RuntimeException("SHA-256 암호화 중 오류 발생", e);
        }
    }
}
