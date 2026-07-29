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
        return parseClaims(token).get(MEMBER_ID_CLAIM, Long.class);
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
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 위조(SignatureException) 포함 모든 JWT 파싱 실패는 클라이언트 귀책이므로 401.
            // T5(500)는 JWT와 무관한 예기치 못한 예외에만 남긴다. (RoleInterceptor 계약: 제시된 토큰 검증 실패 → 401)
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new ApplicationException(TokenExceptionCode.UNKNOWN_TOKEN);
        }
    }
}
