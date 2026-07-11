package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;

    public TokenService(TokenProperty tokenProperty) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(tokenProperty.secretKey()));
        this.accessTokenExpirationMillis = tokenProperty.accessTokenExpirationMillis();
    }

    public Token createToken(Long memberId, Role role) {
        String accessToken = Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        return new Token(accessToken);
    }

    public Long extractMemberId(String token) {
        Claims claims = parseClaims(token);
        Long memberId = claims.get(MEMBER_ID_CLAIM, Long.class);
        if (memberId != null) {
            return memberId;
        }
        // 관용 파서(ADR-0003 Stage 0): 계정 통합(Stage 5) 후 발급되는 토큰은 sub에 계정 id를 담는다.
        // 숫자가 아닌 sub(GeekChat UUID 토큰 등)는 이 서비스의 식별자가 아니므로 null.
        String subject = claims.getSubject();
        if (subject == null) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Role extractRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        return role == null ? null : Role.valueOf(role);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ApplicationException(TokenExceptionCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new ApplicationException(TokenExceptionCode.UNKNOWN_TOKEN);
        }
    }
}
