package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.ExceptionCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private static final String MEMBER_ID_CLAIM = "memberId";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;

    public TokenService(TokenProperty tokenProperty) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(tokenProperty.secretKey()));
        this.accessTokenExpirationMillis = tokenProperty.accessTokenExpirationMillis();
    }

    public Token createToken(Long memberId) {
        String accessToken = Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        return new Token(accessToken);
    }

    public Long extractMemberId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(MEMBER_ID_CLAIM, Long.class);
        } catch (ExpiredJwtException e) {
            throw new ApplicationException(TokenExceptionCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new ApplicationException(TokenExceptionCode.INVALID_TOKEN);
        }
    }
}
