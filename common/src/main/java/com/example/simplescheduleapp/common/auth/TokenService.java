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

    public Token createToken(Long memberId) {
        return createToken(memberId, null);
    }

    public Token createToken(Long memberId, MemberRole role) {
        JwtBuilder builder = Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS512);

        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return new Token(builder.compact());
    }

    public Long extractMemberId(String token) {
        return parseClaims(token).get(MEMBER_ID_CLAIM, Long.class);
    }

    public MemberRole extractRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        if (role == null) {
            return null;
        }
        return MemberRole.valueOf(role);
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
