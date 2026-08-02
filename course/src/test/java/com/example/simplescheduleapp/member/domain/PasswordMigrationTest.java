package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.student.domain.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비밀번호 bcrypt 전환 + 점진 마이그레이션 검증 (ADR-0004, 구조 리뷰 HIGH 보안 수정).
 *
 * <p>무염 SHA-256 → bcrypt 전환의 최대 위험은 <b>기존 사용자 전원 로그인 불가</b>다.
 * 운영 DB의 레거시 해시가 계속 인증되고, 성공 시 bcrypt로 승급되는지 실제로 검증한다.
 * (도메인이 순수해진 덕에 인프라 없이 검증 가능)
 */
@DisplayName("비밀번호 bcrypt 마이그레이션")
class PasswordMigrationTest {

    private static final String PLAIN = "Password123!";

    /** 운영 DB에 남아 있는 형식 — 개선 전 코드가 쓰던 무염 SHA-256 hex. */
    private static String legacySha256Hex(String plain) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private static Student studentWithStoredHash(String storedHash) {
        // 매퍼가 DB 해시로 도메인을 복원하는 경로와 동일(복원용 생성자)
        return new Student(1L, "jungsehui", new Password(storedHash), "정세희", 25, "01023420594", "학교");
    }

    @Test
    void 신규_비밀번호는_bcrypt로_해싱된다() {
        Password password = Password.hashPassword(PLAIN);

        assertThat(password.getHashedPassword())
                .as("bcrypt 해시는 $2 접두사를 가진다")
                .startsWith("$2");
        assertThat(password.isLegacy()).isFalse();
        assertThat(password.match(PLAIN)).isTrue();
        assertThat(password.match("WrongPassword")).isFalse();
    }

    @Test
    void 같은_평문도_매번_다른_해시가_된다_솔트_내장() {
        // 무염 SHA-256의 핵심 취약점(같은 평문 → 같은 해시 → 레인보우 테이블)이 해소됐는지
        String first = Password.hashPassword(PLAIN).getHashedPassword();
        String second = Password.hashPassword(PLAIN).getHashedPassword();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 레거시_SHA256_해시로도_로그인이_계속_된다() throws Exception {
        // 기존 사용자 무중단 — 이게 깨지면 배포 즉시 전원 로그인 불가
        Student member = studentWithStoredHash(legacySha256Hex(PLAIN));

        assertThat(member.getPassword().isLegacy()).isTrue();
        assertThatThrownBy(() -> member.login("WrongPassword"))
                .isInstanceOf(ApplicationException.class);
        // 올바른 평문이면 인증 성공(예외 없음)
        member.login(PLAIN);
    }

    @Test
    void 레거시_해시로_로그인에_성공하면_bcrypt로_승급되고_저장이_요청된다() throws Exception {
        Student member = studentWithStoredHash(legacySha256Hex(PLAIN));

        boolean upgraded = member.login(PLAIN);

        assertThat(upgraded)
                .as("승급되면 애플리케이션 계층이 저장하도록 true를 반환해야 한다")
                .isTrue();
        assertThat(member.getPassword().getHashedPassword()).startsWith("$2");
        assertThat(member.getPassword().isLegacy()).isFalse();
        assertThat(member.getPassword().match(PLAIN))
                .as("승급 후에도 같은 평문으로 인증돼야 한다")
                .isTrue();
    }

    @Test
    void 이미_bcrypt면_승급하지_않는다() {
        Student member = studentWithStoredHash(Password.hashPassword(PLAIN).getHashedPassword());

        boolean upgraded = member.login(PLAIN);

        assertThat(upgraded)
                .as("불필요한 재해시·저장을 하지 않아야 한다")
                .isFalse();
    }
}
