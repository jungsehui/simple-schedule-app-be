package com.example.simplescheduleapp.common.auth;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class BearerTokenExtractor {

    private static final String BEARER_STRING_PREFIX = "Bearer ";
    private static final int BEGIN_INDEX_PREFIX = 7;

    public String extract(String bearerToken) {
        log.info("Bearer 토큰 추출 중 ...");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_STRING_PREFIX)) {
            String token = bearerToken.substring(BEGIN_INDEX_PREFIX);
            log.info("Bearer Token 추출 완료 !!!");
            return token;
        }
        throw new ApplicationException(TokenExceptionCode.REQUIRED_BEARER_TOKEN);
    }
}
